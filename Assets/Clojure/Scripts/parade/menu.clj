(ns parade.menu
	(:use hard.core hard.input unity.messages)
	(:require
			unity.core
            )
	(:import [UnityEngine Application]))
 


(require 'unity.core)
(use 'unity.messages 'hard.core 'hard.input)



(unity.core/defcomponent Menu []
	(Awake [this]
		(use 'hard.core)
		(use 'hard.input)
		(import '[UnityEngine Application]))
	(Update [this]
		(when (key? "space")
			(set! (.text (.GetComponent (find-name "message") "GUIText")) "loading...")
			(Application/LoadLevel "parade"))))