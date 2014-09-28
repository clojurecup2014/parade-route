(ns parade.core
	(:use hard.core hard.input unity.messages)
	(:require
			unity.core
            )
	(:import [UnityEngine Application]))
 


(require 'unity.core)
(use 'unity.messages 'hard.core 'hard.input)
(declare handle-mouse-down set-text! check-mouse-move setup)
(declare brain game-map que pointer tiles tent level-time people score)



 
(def level-time (atom 0))   
(def parade-time (atom 2))
(def score (atom 0))  
(def route (atom [{:t [10 5] :d 3}]))
(def game-over false)

(defn start-game []
	(def level-time (atom 0))   
	(def parade-time (atom 2))
	(def score (atom 0))  
	(def route (atom [{:t [10 5] :d 3}]))
	(def game-over false)
	(set-text! "time-label" "parade starts in:")
	(set! (.color (.GetComponent (find-name "time") "GUIText")) (color 1 0 0))
	(reset! level-time -31)
	(reset! score 0))

(defn op-dir [idx]
	(last (take (+ idx 3) (cycle [0 1 2 3]) )))

(defn right-dir [idx]
	(last (take (+ idx 4) (cycle [0 1 2 3]) )))

(defn left-dir [idx]
	(last (take (+ idx 2) (cycle [0 1 2 3]) )))
 
(defn rnext [h]
	(case (:d h)
		0 (-v + (:t h) [0 1])
		1 (-v + (:t h) [1 0])
		2 (-v + (:t h) [0 -1])
		3 (-v + (:t h) [-1 0])
		nil))

(def bweenmap {0 [0 0] 1 [0 0] 2 [0 0] 3 [0 0]})

(defn validate [head tile]
	(when (= 1 (get (vec (:sig tile)) (op-dir (:d head)))) true))

(defn orient [d sig]
	(let [vsig (vec sig)]
	(cond (= 1 (get vsig d)) d
		(= 1 (get vsig (right-dir d))) (right-dir d)
		(= 1 (get vsig (left-dir d))) (left-dir d)
		:else 0
		)))

(def lookmap {
	0 [1 0 0]
	1 [0 0 -1]
	2 [-1 0 0]
	3 [0 0 1]
	})

(defn inc-route []
	(let [head (first @route)]
		(when-let [nt (rnext head)]
			(if (validate head (get @tiles nt))
				(do
					(swap! route #(vec (cons {:t nt :d (orient (:d head) (:sig (get @tiles nt)))} %)))
					(mapv (fn [r p] 
							(when p
								(let [[tx ty] (-v + (:t r) (get bweenmap (:d r)))]
									(position! p [tx 0 ty])
									(.LookAt (.transform p) (vec3 (-v + (get lookmap (:d r)) (vec3 p))))
									)))
						@route (take (count @route) @people)))
				:game-over ))))

(defn animod [d1 d2 delta]
	(case d1
		0 [0 delta]
		1 [delta 0]
		2 [0 (* delta -1)]
		3 [(* delta -1) 0]))

(defn animate-parade [delta]
	(mapv (fn [r p] 
		(when p
			(let [[mx mz] (animod (:d r) 0 delta)
				[tx ty] (-v + (:t r) (get bweenmap (:d r)))]
				(position! p [(+ tx mx) 0 (+ ty mz)]))))
	@route (take (count @route) @people)))
 

(defn check-mouse-move []
	(position! pointer [0 -1000 0])
	(when-let [hit (first (vec (ray-hits (mouse-ray) 5000)))]
		(let [[x y z] (-v + (unvec (.point hit)) 0.0)
			  tile (mapv int (-v + [x z] [0.5 0.5]))
			  [tx tz] tile]
			  (when-let [target (get @tiles tile)]
			  	(when-not ((set (mapv :t @route)) tile)
			  		(position! pointer [tx 0 tz])
			  		(set-text! "debug" [tile (vec (:sig target))]))))))
	
(def fabs 
	{:empty-tile (resource "empty-tile")
	 :base (resource "base")
	 :road (resource "road")
	 :road-curve (resource "road-curve")
	 :road-intersection (resource "road-intersection")
	 :pointer (resource "pointer")
	 :tent (resource "tent")
	 :people [(resource "p1")
	 (resource "p2")
	 (resource "p3")
	 (resource "p4")
	 (resource "p5")]})

(defn make-tile []
	(let [chosen (rand-nth [:road :road :road-curve :road-curve :road-intersection])
		  base (clone! (:base fabs))
		  road (clone! (chosen fabs))
		  sig (chosen {:road [1 0 1 0]
		  			   :road-curve [0 0 1 1]
		  			   :road-intersection [1 1 1 1]})
		  rot (rand-int 4)]
		  
		  (rotate-around! road (vec3 base) [0 1 0] (* -90 rot))
		  (parent! road base)
		  {:sig (take 4 (drop rot (cycle sig)))
		  	:go base}
		  ))


(defn handle-mouse-down []
	(when-let [hit (first (vec (ray-hits (mouse-ray) 5000)))]
		(let [[x y z] (-v + (unvec (.point hit)) 0.0)
			  tile (mapv int (-v + [x z] [0.5 0.5]))]
			  (when-let [target (get @tiles tile)]
			  	(when-not ((set (mapv :t @route)) tile)
			  		
			  	(if true ;(:empty target)
				  	(let [gob (:go (first @que))
				  		  nt (make-tile)]
				  		;destroy the clicked tile and replace with first in que
				  		(.Play (.GetComponent (find-name "place") "AudioSource"))
				  		(set! (.name gob) (str tile))
				  		(parent! gob game-map)	  	
				  		(local-scale! gob [1 1 1])
				  		(position! gob [(first tile) 0 (last tile)])
				  		(log (gameobject? (:go target)))
				  		(swap! tiles #(conj % {tile (first @que)}))

				  		;add new tile to que and update atoms
				  		(local-scale! (:go nt) [2 2 2])
				  		(position! (:go nt) [-2 -5 5])
				  		(swap! que #(concat (rest %) [nt]))
				  		(mapv 
				  			#(position! (:go %) (-v + (vec3 (:go %)) [0 1 0])) 
				  			 @que)
				  		
				  		
				  		(position! (:go target) [0 -1000 0]))
				  	
				  	))))))

(defn set-text! [n s]
	(set! (.text (.GetComponent (find-name n) "GUIText")) (str s)))
 
(unity.core/defcomponent Brain []
	(Awake [this]
		(use 'hard.core)
		(use 'hard.input)
		(use 'parade.core)
		(import '[UnityEngine Application]))
	(Start [this]
		(setup)
		(start-game))
	(Update [this]
		(when game-over
			(when (key? "space")
			(Application/LoadLevel "menu")))
		(when-not game-over
		(.Rotate (.transform (find-name "background")) (vec3 [0 0.2 0]))
		(swap! level-time #(+ % (Time/deltaTime)))
		(when (= (int @level-time) 0)
			(do (set-text! "time-label" "score")
			(set! (.color (.GetComponent (find-name "time") "GUIText")) (color 1 1 1))))
		(when (pos? @level-time)
			(do
				(swap! parade-time #(- % (Time/deltaTime)))
			
				(when-not (pos? @parade-time)
					(do (reset! parade-time 2)
						;check for :game-over here
						(if (= (inc-route) :game-over)
							(do
								(def game-over true)
								(.Play (.GetComponent (find-name "gameover") "AudioSource"))
								(parent! (clone! (resource "end") [0 4.3872 0]) game-map)
								))

						))
				(animate-parade (* (- 2 @parade-time) 0.5))))
		(set-text! "time" (int @level-time))
		(set-text! "score" (int @score))
		(check-mouse-move)

		(when (mouse-down?) 
			(do
				(handle-mouse-down)
			)))))

 

; (destroy! [brain game-map pointer tent])
; (when que
; 	(map #(destroy! (:go %)) @que))
; (when people
; 	(map destroy! @people))


	; (def brain (GameObject. "brain"))
	; (.AddComponent brain Brain)
(defn setup []
	(def game-map (GameObject. "game-map"))
	(def pointer (clone! (:pointer fabs) [0 -1000 0]))
	(def tent (clone! (:tent fabs) [10 0 5]))

	(def que 
		(atom (doall (for 
			[idx (range 5)
			 :let [t (make-tile)
			      gob (:go t)]]
			 (do
			 	(position! gob [-2 (* idx -1) 5])
			 	(local-scale! gob [2 2 2])
			 	t)))))

	 (def tiles (atom (into {} (for [x (range 10)
	 	   				z (range 10)
	 	   				:let [t (clone! (:empty-tile fabs) [x 0 z])]]
	 	   				(do 
	 	   					(set! (.name t) (str x "-" z))
	 	   					(parent! t game-map)
	 	   					{[x z] {:empty true :go t :sig [0 0 0 0]}} )))))

	(defn make-person []
		(let [me (clone! (rand-nth (:people fabs)) [0 -1000 0])]
			me))

	(def people (atom (vec (take 30 (repeatedly #(make-person)))))))


