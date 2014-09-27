(ns parade.core
	(:use hard.core hard.input unity.messages)
	(:require
			unity.core
            ))
 

(in-ns 'user)
(require 'unity.core)
(use 'unity.messages 'hard.core 'hard.input)

(unity.core/defcomponent Brain []
	(Awake [this]
		(use 'hard.core)
		(use 'hard.input)
		(use 'parade.core))
	(Start [this]
		(log "hard core in parade Brain"))
	(Update [this]
		(when (mouse-down?) (log "mouse down!")))) 
   
 

(def fabs 
	{:empty-tile (resource "empty-tile")
	 :base (resource "base")
	 :road (resource "road")
	 :road-curve (resource "road-curve")
	 :road-intersection (resource "road-intersection")})

(defn make-tile []
	(let [chosen (rand-nth [:road :road-curve :road-intersection])
		  base (clone! (:base fabs))
		  road (clone! (chosen fabs))
		  sig (chosen {:road [1 0 1 0]
		  			   :road-curve [0 0 1 1]
		  			   :road-intersection [1 1 1 1]})
		  rot (rand-int 4)]
		  (parent! road base)
		  {:sig (take 4 (drop rot (cycle sig)))
		  	:go base}
		  ))

(declare brain game-map)
(destroy! [brain game-map])
(def brain (GameObject. "brain"))
(.AddComponent brain Brain)
(def game-map (GameObject. "game-map"))

(def que 
	(doall (for 
		[idx (range 5)
		 :let [t (make-tile)]]
		 (do
		 	(position! t [-2 (* idx -1) 5])
		 	(local-scale! t [2 2 2])
		 	t))))

 (def tiles (into {} (for [x (range 10)
 	   				z (range 10)
 	   				:let [t (clone! (:empty-tile fabs) [x 0 z])]]
 	   				(do 
 	   					(set! (.name t) (str x "-" z))
 	   					(parent! t game-map)
 	   					{[x z] {:go t :sig [0 0 0 0]}})))])
  