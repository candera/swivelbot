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

;; Not sure this next line is necessary:
(com.pi4j.wiringpi.Gpio/wiringPiSetup)

(def pwm (.provisionPwmOutputPin gpio
                                 RaspiPin/GPIO_01 ; #18
                                 "Motor Speed"
                                 0      ; Default value
                                 ))

(def i1 (.provisionDigitalOutputPin gpio
                                    RaspiPin/GPIO_00 ; #21/27
                                    "I1"
                                    PinState/LOW))

(def i2 (.provisionDigitalOutputPin gpio
                                    RaspiPin/GPIO_07 ; #22
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
  (doseq [x (lazy-cat (range 600 1024 8) (range 1024 650 -8))]
    (.setPwm pwm x)
    (Thread/sleep 10)))

(do (dotimes [_ 1000]
       (.setPwm pwm 0)
       (.setPwm pwm 1024))
    (.setPwm pwm 512))

(.setPwm pwm 1024)

(.setPwm pwm 0)
(.setPwm pwm 250)
(.setPwm pwm 800)

(.getProperties pwm)
(.getMode pwm)
(.setMode pwm com.pi4j.io.gpio.PinMode/PWM_OUTPUT)

(.setPwm pwm 100)

(.high i1)
(.low i2)
(.high i2)

(do
  (.toggle i1)
  (.toggle i2))

(defn go [speed]
  (if (< speed 0)
    (do
      (.low i1)
      (.high i2))
    (do
      (.low i2)
      (.high i1)))

  (.setPwm pwm (-> (Math/abs speed) (/ 100.0) (* (- 1024 660)) (+ 660) long)))

(defn stop []
  (.low i1)
  (.low i2))

(-> 20 (/ 100.0) (* (- 1024 660)) (+ 660) long)

(go 10)
(stop)

(.high i2)
(.low i1)

;; Wiring info:
;; Brown = Black = Ground
;; Orange = Red = Positive
;; Yellow = White = Signal

;;; Servo control

(.setPwm pwm 1000)

(dotimes [_ 1000]
  (.setPwm pwm 15)
  (Thread/sleep 1))

(doseq [i (range 0 1024)]
  (.setPwm pwm i)
  (Thread/sleep 10))

;; Software PWM

(com.pi4j.wiringpi.Gpio/wiringPiSetup)

(def i1 (.provisionDigitalOutputPin gpio
                                    RaspiPin/GPIO_00 ; #21/27
                                    "I1"
                                    PinState/LOW))

(def pwm (com.pi4j.wiringpi.SoftPwm/softPwmCreate 1 ; #21/27
                                                  (int 100)
                                                  (int 200)))

(com.pi4j.wiringpi.SoftPwm/softPwmWrite 1 (int 20))

(doseq [n (take 1000 (cycle (range 10 20 1)))]
  (com.pi4j.wiringpi.SoftPwm/softPwmWrite 1 (int n))
  (Thread/sleep 500))

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Java I2C

(def dev (i2c-device 0x40))

(pca9685-read-register dev 56)

(pca9685-set-pwm-freq dev 50)

(pca9685-set-led-cycle dev 12 500)

(pca9685-set-pwm-freq dev 60)
(dotimes [_ 20]
  (pca9685-set-led-cycle dev 12 150)
  (Thread/sleep 1000)
  (pca9685-set-led-cycle dev 12 600)
  (Thread/sleep 1000))

(doseq [i (range 250 510 10)]
  (pca9685-set-led-cycle dev 12 i)
  (Thread/sleep 100))