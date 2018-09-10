(ns checklist.util)


(defn hexify "Convert byte sequence to hex string" [coll]
  (let [hex [\0 \1 \2 \3 \4 \5 \6 \7 \8 \9 \a \b \c \d \e \f]]
      (letfn [(hexify-byte [b]
        (let [v (bit-and b 0xFF)]
          [(hex (bit-shift-right v 4)) (hex (bit-and v 0x0F))]))]
        (apply str (mapcat hexify-byte coll)))))


(defn hexify-str [s]
  (hexify (.getBytes s)))


(defn unhexify "Convert hex string to byte sequence" [s]
      (letfn [(unhexify-2 [c1 c2]
                 (unchecked-byte
                   (+ (bit-shift-left (Character/digit c1 16) 4)
                      (Character/digit c2 16))))]
     (map #(apply unhexify-2 %) (partition 2 s))))


(defn unhexify-str [s]
  (apply str (map char (unhexify s))))
