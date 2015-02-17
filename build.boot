(set-env!
 :source-paths #{"src" "test"}
  :asset-paths    #{"assets"}
  :target-path    "resources/public"
 :dependencies '[[org.clojure/clojure             "1.6.0"     :scope "provided"]
                 [adzerk/bootlaces                "0.1.9"     :scope "test"]
                 [adzerk/boot-test                "1.0.3"     :scope "test"]
                 [jumblerg/ring.middleware.cors   "1.0.1"     :scope "test"]
                 [ring/ring-jetty-adapter         "1.3.2"     :scope "test"]
                 [ring/ring                       "1.3.2"     :scope "test"]])

(require
 '[adzerk.bootlaces :refer :all] ;; tasks: build-jar push-snapshot push-release
 '[adzerk.boot-test :refer :all]
 '[tailrecursion.boot-ring :refer :all])

(def +version+ "0.1.0")

(bootlaces! +version+)

(task-options!
 pom {:project     'tailrecursion/boot-ring
      :version     +version+
      :description "ring tasks for boot"
      :url         "https://github.com/rwillig/boot-ring"
      :scm         {:url "https://github.com/boot-ring"}
      :license     {"" ""}})
