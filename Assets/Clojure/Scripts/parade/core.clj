(ns parade.core
	(:use hard.core hard.input unity.messages)
	(:require
			unity.core
            ))
 
(defn abc [] (log "scripts ns works"))

(unity.core/defcomponent Brain []
	(Awake [this]
		(use 'hard.core)
		(use 'hard.input)
		(use 'parade.core))
	(Start [this]
		(log "hard core in parade Brain")
		(abc))
	(Update [this]
		)) 
   
 
  