(ns parade.core
	(:use hard.core hard.input unity.messages)
	(:require
			unity.core
            ))
 


(unity.core/defcomponent Brain []
	(Awake [this]
		(use 'hard.core)
		(use 'hard.input)
		(use 'parade.core))
	(Start [this]
		(log "hard core in parade Brain"))
	(Update [this]
		)) 
   
 
 (in-ns 'user)

 (let [empty-tile (resource "empty-tile")
 		game-map (GameObject. "game-map")
 	   tiles (for [x (range 10)
 	   				z (range 10)
 	   				:let [t (clone! empty-tile [x 0 z])]]
 	   				(do 
 	   					(set! (.name t) (str x "-" z))
 	   					(parent! t game-map)))]
 	   tiles)
  