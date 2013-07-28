(import '[com.pi4j.io.gpio GpioFactory PinState RaspiPin])

(def gpio (GpioFactory/getInstance))
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