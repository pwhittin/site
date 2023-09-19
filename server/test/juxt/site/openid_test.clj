;; Copyright © 2022, JUXT LTD.

(ns juxt.site.openid-test
  (:require
   [clojure.test :refer [deftest is use-fixtures]]
   [juxt.site.repl :as repl]
   [ring.util.codec :as codec]
   [juxt.site.test-helpers.login :refer [lookup-session-details]]
   [juxt.site.test-helpers.xt :refer [system-xt-fixture]]
   [juxt.site.test-helpers.handler :refer [*handler* handler-fixture]]
   [juxt.site.test-helpers.fixture :refer [with-fixtures]]))

(use-fixtures :each system-xt-fixture handler-fixture)

;;deftest openid-test

;; This is awaiting a mock backing implementation in order to avoid making real
;; http requests to Auth0 and the need to use an actual client-id and secret.
(comment
  (with-fixtures
    (let [parameters
          {"issuer" "https://juxt.eu.auth0.com"
           "client-id" "d8X0TfEIcTl5oaltA4oy9ToEPdn5nFUK"
           "client-secret" "REDACTED"
           "redirect-uri" "https://example.org/openid/callback"}]

      #_(install-bundles! [["juxt/site/bootstrap" {}]
                         ["juxt/site/core" {}]
                         ["juxt/site/openid-client" {}]])

      (repl/ls)
      (repl/e "https://site.test/login")

      ;; TODO: Add the login
      (let [login-request
            {:ring.request/method :get
             :juxt.site/uri "https://site.test/login"
             :ring.request/query (codec/form-encode {"return-to" "/index.html"})}
            login-response (*handler* login-request)
            status (:ring.response/status login-response)

            {:strs [location set-cookie]} (:ring.response/headers login-response)

            {query-param-state "state" :as location-params}
            (when location
              (let [[_ qs] (re-matches #"https.*?\?(.*)" location)]
                (codec/form-decode qs)))
            [_ session-token] (re-matches #"sid=(.*?);.*" set-cookie)

            {:keys [session]} (lookup-session-details session-token)
            {stored-state :juxt.site/state} session

            ]

        (is (= 303 status))

        (is (= (get parameters "client-id")
               (get location-params "client_id")))

        (is (= (get parameters "redirect-uri")
               (get location-params "redirect_uri")))

        (is (= query-param-state stored-state))

        (let [callback-request
              {:ring.request/method :get
               :juxt.site/uri "https://site.test/openid/callback"
               :ring.request/query
               (codec/form-encode
                {"code" "123456"
                 "state" query-param-state})}]
          (*handler* callback-request))

        ;;(hc/get location {:timeout 2000})

        ;; Now we go to the location
        #_(let [authorize-request
                {:ring.request/method :get
                 :juxt.site/uri "https://site.test/login"
                 :ring.request/query (codec/form-encode {"code" "1234" "state" query-param-state})
                 :ring.request/headers {"cookie" cookie-header-value}}])
        ;;login-response

        #_(let [cookie-header-value (format "sid=%s" session-token)
                callback-request
                {:ring.request/method :get
                 :ring.request/path "/openid/callback"
                 :ring.request/query (codec/form-encode {"code" "1234" "state" query-param-state})
                 :ring.request/headers {"cookie" cookie-header-value}}]


            )


        ;; Doesn't have a session-scope, why not?


        ;; What's the code that can dig out a session?


        ))))
