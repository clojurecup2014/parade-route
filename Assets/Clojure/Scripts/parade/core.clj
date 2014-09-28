(ns parade.core
	(:use hard.core hard.input unity.messages)
	(:require
			unity.core
            ))
 


(require 'unity.core)
(use 'unity.messages 'hard.core 'hard.input)
(declare handle-mouse-down)
(declare brain game-map que pointer tiles)

   
 

(defn handle-mouse-down []
	(when-let [hit (first (vec (ray-hits (mouse-ray) 5000)))]
		(let [[x y z] (-v + (unvec (.point hit)) 0.0)
			  tile (mapv int (-v + [x z] [0.5 0.5]))]
			  (position! pointer [x y z])
			  (when-let [target (get @tiles tile)]

			  	(let [gob (:go (first @que))
			  		  nt (make-tile)]
			  		;destroy the clicked tile and replace with first in que
			  		(log target)	
			  		(set! (.name gob) (str tile))
			  		(parent! gob game-map)	  	
			  		(local-scale! gob [1 1 1])
			  		(position! gob [(first tile) 0 (last tile)])
			  		(log (gameobject? (:go target)))
			  		

			  		;add new tile to que and update atoms
			  		(local-scale! (:go nt) [2 2 2])
			  		(position! (:go nt) [-2 -5 5])
			  		(swap! que #(concat (rest %) [nt]))
			  		(mapv 
			  			#(position! (:go %) (-v + (vec3 (:go %)) [0 1 0])) 
			  			 @que)
			  		
			  		(swap! tiles #(conj % {tile nt}))
			  		(position! (:go target) [0 -1000 0])
			  			

			  	)))))


(unity.core/defcomponent Brain []
	(Awake [this]
		(use 'hard.core)
		(use 'hard.input)
		(use 'parade.core))
	(Start [this]
		(log "hard core in parade Brain"))
	(Update [this]
		(when (mouse-down?) 
			(do
				(log (Time/deltaTime))
				(handle-mouse-down)
			))) )

(def fabs 
	{:empty-tile (resource "empty-tile")
	 :base (resource "base")
	 :road (resource "road")
	 :road-curve (resource "road-curve")
	 :road-intersection (resource "road-intersection")
	 :pointer (resource "pointer")})

(defn make-tile []
	(let [chosen (rand-nth [:road :road-curve :road-intersection])
		  base (clone! (:base fabs))
		  road (clone! (chosen fabs))
		  sig (chosen {:road [1 0 1 0]
		  			   :road-curve [0 0 1 1]
		  			   :road-intersection [1 1 1 1]})
		  rot (rand-int 4)]
		  
		  (rotate-around! road (vec3 base) [0 1 0] (* 90 rot))
		  (parent! road base)
		  {:sig (take 4 (drop rot (cycle sig)))
		  	:go base}
		  ))


(destroy! [brain game-map pointer])
(when que
	(map #(destroy! (:go %)) @que))
(def brain (GameObject. "brain"))
(.AddComponent brain Brain)
(def game-map (GameObject. "game-map"))
(def pointer (clone! (:pointer fabs) [0 -1000 0]))

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
 	   					{[x z] {:go t :sig [0 0 0 0]}} )))))

 (doall
 	(for [iter (range 10)
 	   :let [pos [(rand-int 10)
 	   			  (rand-int 10)]]]
 	   			  (when-let [t (get @tiles pos)]
 	   			  	(destroy! (:go t))

 	   			  	(let [nt (make-tile)]
 	   			  		(swap! tiles conj {pos nt})
 	   			  		(position! (:go nt) [(first pos) 0 (last pos)])
 	   			  		(parent! (:go nt) game-map)))))
  