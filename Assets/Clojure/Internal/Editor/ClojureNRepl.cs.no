using UnityEngine;
using UnityEditor;
using clojure.lang;
using System.Collections.Generic;
using System;
using System.Threading;
using System.Reflection;
using System.Reflection.Emit;

public class ClojureNRepl : EditorWindow {

	static String CLJ_PATH = "unity/nrepl/nrepl-editor";
	String CLJ_PKG = "unity.nrepl.nrepl-editor";

    [MenuItem ("Window/Clojure nREPL")]
    static void Init () {
		EditorWindow.GetWindow(typeof(ClojureNRepl));
		//RT.load(CLJ_PATH);
    }
    
    void OnGUI () {
		RT.var(CLJ_PKG, "on-gui").invoke(this);
    }
	
	//void OnFocus () {
	//	RT.var(CLJ_PKG, "on-focus").invoke();
	//}

	void OnHeirarchyChange() {
		RT.var(CLJ_PKG, "on-heirarchy-change").invoke();
	}

	//void OnInspectorUpdate() {
		//Log ("OnInspectorUpdate");
	//	RT.var(CLJ_PKG, "on-inspector-update").invoke();
	//}

	//void OnLostFocus() {
	//	RT.var(CLJ_PKG, "on-lost-focus").invoke();
	//}

	void OnProjectChange() {
		RT.var(CLJ_PKG, "on-project-change").invoke();
	}

	void OnSelectionChange() {
		RT.var(CLJ_PKG, "on-selection-change").invoke();
	}

	void Update() {
		RT.var(CLJ_PKG, "update").invoke();
	}

	void OnDestroy() {
		RT.var(CLJ_PKG, "on-destroy").invoke();
	}

	void OnDisable() {
		RT.var(CLJ_PKG, "on-disable").invoke();
	}

	void OnEnable() {
		RT.load(CLJ_PATH);
	}
}
