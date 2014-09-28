(ns hard.input
  (:import
    [UnityEngine Input KeyCode Camera Physics Time]))
  
(declare mouse?)
 
(defn get-keycode [s]
	(try (or (eval (symbol (str "KeyCode/" s))) false) (catch Exception e nil)))

(defn ^:private kcode* [k]
	(cond 
		(keyword? k) (apply str (rest (str k)))
		(symbol? k) (get-keycode k)
		(string? k) k))

(defn key-down? [k]
	(Input/GetKeyDown (kcode* k)))

(defn key? [k]
	(Input/GetKey (kcode* k)))

(defn key-up? [k]
	(Input/GetKeyUp (kcode* k)))

   

(defn ^:private mouse-code* [b]
	(cond (#{0 1 2} b) b
		:else (or (get {:left 0 :middle 1 :right 2} b) 0)))

(defn mouse-down? 
	([] (mouse-down? 0)) 
	([b] (Input/GetMouseButtonDown (mouse-code* b))))

(defn mouse-up? 
	([] (mouse-up? 0)) 
	([b] (Input/GetMouseButtonUp (mouse-code* b))))

(defn mouse? 
	([] (mouse? 0)) 
	([b] (Input/GetMouseButton (mouse-code* b))))


(defn mouse-pos []
	(let [pos (Input/mousePosition)]
		[(.x pos) (.y pos)]))


(defn get-axis [k]
	(case k
		:horizontal (Input/GetAxis "Horizontal")
		"horizontal" (Input/GetAxis "Horizontal")
		:vertical (Input/GetAxis "Vertical")
		"vertical" (Input/GetAxis "Vertical"
		:mouse-x (Input/GetAxis "Mouse X")
		"Mouse X" (Input/GetAxis "Mouse X")
		:mouse-y (Input/GetAxis "Mouse Y")
		"Mouse Y" (Input/GetAxis "Mouse Y"))))

(defn mouse-ray []
	(.ScreenPointToRay (Camera/main) (Input/mousePosition)))

(defn ray-hit
	([ray] (ray-hit ray 10000))
	([^Ray ray ^double length ]
		(let [hit (RaycastHit.)]
			(Physics/Raycast ray (by-ref hit) length))))

(defn ray-hits
	;([ray] (ray-hit ray 100))
	([^Ray ray ^double length ]
		(let [hit (RaycastHit.)]
			;(Physics/Raycast ray hit (float 100) (int 0))
			(Physics/RaycastAll (.origin ray) (.direction ray) length)
			)))


