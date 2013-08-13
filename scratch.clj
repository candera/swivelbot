;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def pin (.provisionDigitalOutputPin gpio
                                     RaspiPin/GPIO_17
                                     "MyLED"
                                     PinState/HIGH))

(doseq [pin (.getProvisionedPins gpio)] (.unprovisionPin pin))

(def pin (nth (.getProvisionedPins gpio) 0))

pin

(.low pin)
(.high pin)
(.toggle pin)
(def f (.blink pin 250))
(future-cancel f)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def pwm (.provisionPwmOutputPin gpio
                                 RaspiPin/GPIO_01 ; #18
                                 "Motor Speed"
                                 0      ; Default value
                                 ))

(def i1 (.provisionDigitalOutputPin gpio
                                    RaspiPin/GPIO_02 ; #21/27
                                    "I1"
                                    PinState/LOW))

(def i2 (.provisionDigitalOutputPin gpio
                                    RaspiPin/GPIO_03 ; #22
                                    "I2"
                                    PinState/LOW))

(def done (atom false))

(def f
  (let [done (atom false)]
    (future
      (loop [[x & more] (cycle (range 0 1024 100))]
        (.setPwm pwm (first x))
        (Thread/sleep 10)
        (when (not done) (recur (rest x)))))
    done))

(reset! f true)

(dotimes [_ 4]
  (doseq [x (lazy-cat (range 0 1024 8) (range 1024 0 -8))]
    (.setPwm pwm x)
    (Thread/sleep 10)))

(do (dotimes [_ 1000]
       (.setPwm pwm 0)
       (.setPwm pwm 1024))
    (.setPwm pwm 512))

(.setPwm pwm 1024)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; OSC stuff

(import '[com.illposed.osc OSCPortIn OSCListener])
(def receiver (OSCPortIn. 9000))

(def listener (reify OSCListener
                (acceptMessage [this time message]
                  (println "message received"
                           time
                           (.getAddress message)
                           (map str (.getArguments message))))))

(.addListener receiver "/1/fader1" listener)
(.addListener receiver "/accxyz" listener)

(.startListening receiver)

(def run-f (future (.run receiver)))

(.stopListening receiver)
