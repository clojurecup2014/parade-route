(ns ^{:doc "Support for persistent, cross-connection REPL sessions."
      :author "Chas Emerick"}
  unity.nrepl.middleware.session
  (:use [clojure.tools.nrepl.misc :only (uuid response-for returning log)]
        [unity.nrepl.middleware.interruptible-eval :only (*msg*)]
        [clojure.tools.nrepl.middleware :only (set-descriptor!)])
  (:require (clojure main test)
            [clojure.tools.nrepl.debug :as debug]
            [clojure.tools.nrepl.transport :as t])
  (:import clojure.tools.nrepl.transport.Transport
           (System.IO TextReader TextWriter StringReader StreamWriter StringWriter)
           clojure.lang.LineNumberingTextReader
           ))

(def ^{:private true} sessions (atom {}))

;; TODO the way this is currently, :out and :err will continue to be
;; associated with a particular *msg* (and session) even when produced from a future,
;; agent, etc. due to binding conveyance.  This may or may not be desirable
;; depending upon the expectations of the client/user.  I'm not sure at the moment
;; how best to make it configurable though...

(def ^{:dynamic true :private true} *out-limit* 1024)
(def ^{:dynamic true :private true} *skipping-eol* false)

(defn- session-out
  "Returns a PrintWriter suitable for binding as *out* or *err*.  All of
   the content written to that PrintWriter will (when .flush-ed) be sent on the
   given transport in messages specifying the given session-id.
   `channel-type` should be :out or :err, as appropriate."
  [channel-type session-id transport]
  (let [buf (StringBuilder.)
        maybe-flush (fn [^StringWriter w ^StringBuilder buf]
                      (when (or (<= *out-limit* (.Length buf))
                                (.Contains (.ToString buf) (Environment/NewLine)))
                        (.Flush  w)))
        newline (Environment/NewLine)
        nl-len (.Length newline)
        send-segment (fn [^String segment]
                       #_(debug/prn-thread "Sending " segment)
                       (t/send (or (:transport *msg*) transport)
                               (response-for *msg* :session session-id
                                             channel-type segment)))
        send-text (fn [^String text]
                    (let [text-len (.Length text)]
                      (when (pos? text-len)
                        (loop [start 0]
                          (let [idx (.IndexOf text newline start)]
                            (if (neg? idx)
                              (send-segment (.Substring text start)) ; no more newlines, just spit out remainder
                              (let [idx2 (+ idx nl-len)]
                                (send-segment (.Substring text start (- idx2 start)))
                                (when (< idx2 text-len)
                                  (recur idx2)))))))))]
    (identity (proxy [StringWriter] [buf]
                (Dispose [isDisposing] (when isDisposing (.Flush ^TextWriter this)))
                (Write
                  ([x] (locking buf
                         (proxy-super Write x)
                         (maybe-flush this buf)))
                  ([ x y ] (locking buf
                             (proxy-super Write x y)
                             (maybe-flush this buf)))
                  ([ x y z] (locking buf
                              (proxy-super Write x y z)
                              (maybe-flush this buf)))
                  ([ x y z w] (locking buf
                                (proxy-super Write x y z w)
                                (maybe-flush this buf))))
                (WriteLine
                  ([] (locking buf
                        (proxy-super WriteLine)
                        (maybe-flush this buf)))
                  ([x] (locking buf
                         (proxy-super WriteLine x)
                         (maybe-flush this buf)))
                  ([ x y ] (locking buf
                             (proxy-super WriteLine x y)
                             (maybe-flush this buf)))
                  ([ x y z] (locking buf
                              (proxy-super WriteLine x y z)
                              (maybe-flush this buf)))
                  ([ x y z w] (locking buf
                                (proxy-super WriteLine x y z w)
                                (maybe-flush this buf))))
                (Flush []
                  (let [text (locking buf (let [text (str buf)]
                                            (.set_Length buf 0)
                                            text))]
                    (send-text text)))))))


(defn- session-in
  "Returns a LineNumberingPushbackReader suitable for binding to *in*.
   When something attempts to read from it, it will (if empty) send a
   {:status :need-input} message on the provided transport so the client/user
   can provide content to be read."
  [session-id transport]
  (let [input-queue (|System.Collections.Generic.List`1[System.Object]|.)
        request-input (fn []
                        #_(debug/prn-thread "Request input")
                        (cond (> (.Count input-queue) 0)
                              (.Take input-queue)
                              *skipping-eol*
                              nil
                              :else
                              (do
                                #_(debug/prn-thread "Sending message")
                                (t/send transport
                                        (response-for *msg* :session session-id
                                                      :status :need-input))
                                (.Take input-queue))))
        do-read (fn [buf off len]
                  #_(debug/prn-thread "do-read")
                  (locking input-queue
                    (loop [i off]
                      (cond
                       (>= i (+ off len))
                       (+ off len)
                       (> (.Count input-queue) 0)
                       (do (aset-char buf i (char (.Take input-queue)))
                           (recur (inc i)))
                       :else
                       i))))
        reader (LineNumberingTextReader.
                (proxy [TextReader] []
                  (Dispose [disposing] (when disposing (.Dispose input-queue)))
                  (Peek [] -1)
                  (Read
                    ([]
                       #_(debug/prn-thread "Read[]")
                       (let [first-character (request-input)]
                         (if (or (nil? first-character) (= first-character -1))
                           (int -1)
                           (int first-character))))
                    ([^chars buf off len]
                       #_(debug/prn-thread "Read[3]")
                       (if (zero? len)
                         -1
                         (let [first-character (request-input)]
                           (if (or (nil? first-character) (= first-character -1))
                             -1
                             (do
                               (aset-char buf off (char first-character))
                               (- (do-read buf (inc off) (dec len))
                                  off)))))))))]
    {:input-queue input-queue
     :stdin-reader reader}))

(defn- create-session
  "Returns a new atom containing a map of bindings as per
   `clojure.core/get-thread-bindings`.  Values for *out*, *err*, and *in*
   are obtained using `session-in` and `session-out`, *ns* defaults to 'user,
   and other bindings as optionally provided in `baseline-bindings` are
   merged in."
  ([transport] (create-session transport {}))
  ([transport baseline-bindings]
     (clojure.main/with-bindings
       (let [id (uuid)
             out (session-out :out id transport)
             {:keys [input-queue stdin-reader]} (session-in id transport)]
         (binding [*out* out
                   *err* (session-out :err id transport)
                   *in* stdin-reader
                   *ns* (create-ns 'user)
                   *out-limit* (or (baseline-bindings #'*out-limit*) 1024)
                                        ; clojure.test captures *out* at load-time, so we need to make sure
                                        ; runtime output of test status/results is redirected properly
                                        ; TODO is this something we need to consider in general, or is this
                                        ; specific hack reasonable?
                   clojure.test/*test-out* out]
                                        ; nrepl.server happens to use agents for connection dispatch
                                        ; don't capture that *agent* binding for userland REPL sessions
           (atom (merge baseline-bindings (dissoc (get-thread-bindings) #'*agent*))
                 :meta {:id id
                        :stdin-reader stdin-reader
                        :input-queue input-queue}))))))

(defn- register-session
  "Registers a new session containing the baseline bindings contained in the
   given message's :session."
  [{:keys [session transport] :as msg}]
  (let [session (create-session transport @session)
        id (-> session meta :id)]
    (swap! sessions assoc id session)
    (t/send transport (response-for msg :status :done :new-session id))))

(defn- close-session
  "Drops the session associated with the given message."
  [{:keys [session transport] :as msg}]
  #_(debug/prn-thread "close-session" (-> session meta :id)) ;DEBUG
  (swap! sessions dissoc (-> session meta :id))
  (t/send transport (response-for msg :status #{:done :session-closed})))

(defn session
  "Session middleware.  Returns a handler which supports these :op-erations:

   * \"ls-sessions\", which results in a response message
     containing a list of the IDs of the currently-retained sessions in a
     :session slot.
   * \"close\", which drops the session indicated by the
     ID in the :session slot.  The response message's :status will include
     :session-closed.
   * \"clone\", which will cause a new session to be retained.  The ID of this
     new session will be returned in a response message in a :new-session
     slot.  The new session's state (dynamic scope, etc) will be a copy of
     the state of the session identified in the :session slot of the request.

   Messages indicating other operations are delegated to the given handler,
   with the session identified by the :session ID added to the message. If
   no :session ID is found, a new session is created (which will only
   persist for the duration of the handling of the given message).

   Requires the interruptible-eval middleware (specifically, its binding of
   *msg* to the currently-evaluated message so that session-specific *out*
   and *err* content can be associated with the originating message)."
  [h]
  (fn [{:keys [op session transport out-limit] :as msg}]
    (let [the-session (if session
                        (@sessions session)
                        (create-session transport))]
      (if-not the-session
        (t/send transport (response-for msg :status #{:error :unknown-session}))
        (let [msg (assoc msg :session the-session)]
          ;; TODO yak, this is ugly; need to cleanly thread out-limit through to
          ;; session-out without abusing a dynamic var
          ;; (there's no reason to allow a connected client to fark around with
          ;; a session-out's "buffer")
          (when out-limit (swap! the-session assoc #'*out-limit* out-limit))
          (case op
            "clone" (register-session msg)
            "close" (close-session msg)
            "ls-sessions" (t/send transport (response-for msg :status :done
                                                          :sessions (or (keys @sessions) [])))
            (h msg)))))))

(set-descriptor! #'session
                 {:requires #{}
                  :expects #{}
                  :handles {"close"
                            {:doc "Closes the specified session."
                             :requires {"session" "The ID of the session to be closed."}
                             :optional {}
                             :returns {}}
                            "ls-sessions"
                            {:doc "Lists the IDs of all active sessions."
                             :requires {}
                             :optional {}
                             :returns {"sessions" "A list of all available session IDs."}}
                            "clone"
                            {:doc "Clones the current session, returning the ID of the newly-created session."
                             :requires {}
                             :optional {"session" "The ID of the session to be cloned; if not provided, a new session with default bindings is created, and mapped to the returned session ID."}
                             :returns {"new-session" "The ID of the new session."}}}})

(defn add-stdin
  "stdin middleware.  Returns a handler that supports a \"stdin\" :op-eration, which
   adds content provided in a :stdin slot to the session's *in* Reader.  Delegates to
   the given handler for other operations.

   Requires the session middleware."
  [h]
  (fn [{:keys [op stdin session transport] :as msg}]
    (cond
     (= op "eval")
     (let [in (-> (meta session) ^LineNumberingTextReader (:stdin-reader))]                 ;DM: LineNumberingPushbackReader
       (binding [*skipping-eol* true]
         (clojure.main/skip-if-eol in))
       (h msg))
     (= op "stdin")
     (let [q (-> (meta session) ^TextWriter (:input-queue))]                                ;DM: ^Writer
       (locking q
         (doseq [c stdin] (.Add q c)))                                                      ;DM: .put
       (t/send transport (response-for msg :status :done)))
     :else
     (h msg))))

(set-descriptor! #'add-stdin
                 {:requires #{#'session}
                  :expects #{"eval"}
                  :handles {"stdin"
                            {:doc "Add content from the value of \"stdin\" to *in* in the current session."
                             :requires {"stdin" "Content to add to *in*."}
                             :optional {}
                             :returns {"status" "A status of \"need-input\" will be sent if a session's *in* requires content in order to satisfy an attempted read operation."}}}})
