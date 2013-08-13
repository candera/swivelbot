(ns user
  "Put things in here you want available at the REPL at development
  time. Will not be included in production code."
  (:require [clojure.tools.namespace.repl :refer [refresh]])
  (:import [com.pi4j.io.gpio GpioFactory PinState RaspiPin]
           [com.pi4j.io.i2c I2CFactory I2CBus I2CDevice]))

(def gpio (GpioFactory/getInstance))

(def i2c-busses
  {0 I2CBus/BUS_0
   1 I2CBus/BUS_1})

(def pca9685-registers
  {:subadr1 0x02
   :subadr2 0x03
   :subadr3 0x04
   :mode1   0x00
   :prescale 0xFE
   :led0-on-l 0x06
   :led0-on-h 0x07
   :led0-off-l 0x08
   :led0-off-h 0x09})

(defn ubyte
  "Returns a byte that, when intepreted as an unsigned byte, is equal to n."
  [n]
  (if (< 127 n)
    (byte (- n 128))
    (byte n)))

(defn i2c-device
  "Return an I2C device object with address `address`."
  ^I2CDevice [address]
  (-> (I2CFactory/getInstance I2CBus/BUS_1) (.getDevice address)))

(defn pca9685-write-register
  "Writes a single byte `value` to `register` on `device`"
  [^I2CDevice device register value]
  (.write device
          (if (keyword? register)
            (int (pca9685-registers register))
            (int register))
          (ubyte value)))

(defn pca9685-read-register
  "Reads a single bye from `register` on `device`"
  [^I2CDevice device register]
  (.read device (if (keyword? register)
                  (int (pca9685-registers register))
                  (int register))))

(defn pca9685-set-pwm-freq
  "Sets the PWM frequency to `freq` Hz."
  [^I2CDevice device freq]
  (let [prescale (-> 25000000.0 (/ 4096) (/ freq) (- 1) (+ 0.5) (Math/floor))
        old-mode (pca9685-read-register device :mode1)
        new-mode (-> old-mode (bit-and 0x7F) (bit-or 0x10)) ; sleep
        ]
    (println "Setting prescale to" (int prescale))
    (pca9685-write-register device :mode1 new-mode)
    (pca9685-write-register device :prescale (int prescale))
    (pca9685-write-register device :mode1 old-mode)
    (Thread/sleep 5)
    (pca9685-write-register device :mode1 (bit-or old-mode 0x80))))

(defn pca9685-set-led-cycle
  "Sets up the pca9685 to output a PWM that cycles LED number `led` on
  for `on` (out of 4096) of the pulse cycle."
  [^I2CDevice device led on]
  (pca9685-write-register device
                          (+ (:led0-on-l pca9685-registers) (* 4 led))
                          0)
  (pca9685-write-register device
                          (+ (:led0-on-h pca9685-registers) (* 4 led))
                          0)
  (pca9685-write-register device
                          (+ (:led0-off-l pca9685-registers) (* 4 led))
                          (bit-and on 0xFF))
  (pca9685-write-register device
                          (+ (:led0-off-h pca9685-registers) (* 4 led))
                          (-> on (bit-shift-right 8) (bit-and 0x0F))))