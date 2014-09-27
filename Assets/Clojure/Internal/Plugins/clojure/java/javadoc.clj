(ns clojure.java.javadoc
  (:import (UnityEngine Debug)))

(defn clrdoc-url [class-name]
  (str "http://google.com/q=" class-name))

(defn browse-url [url]
  (Debug/Log (str "browsing url: " url)))

(defn javadoc
  "Opens a browser window displaying the javadoc for the argument.
  Tries *local-javadocs* first, then *remote-javadocs*."
  {:added "1.2"}
  [class-or-object]
  (let [^Class c (if (instance? Class class-or-object)
                    class-or-object
                    (class class-or-object))]
    (if-let [url (clrdoc-url (.getName c))]
      (browse-url url)
      (Debug/Log "Could not find CLR doc for" c))))
