(ns user
  "Put things in here you want available at the REPL at development
  time. Will not be included in production code."
  (:import [com.pi4j.io.gpio GpioFactory PinState RaspiPin])
)

(def gpio (GpioFactory/getInstance))


