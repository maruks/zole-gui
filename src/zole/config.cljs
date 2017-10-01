(ns zole.config)

(def debug? ^boolean goog.DEBUG)

(goog-define dynamic-ws-port false)

(def default-ws-port 8080)

(defn ws-port-number []
  (if dynamic-ws-port js/window.location.port default-ws-port))
