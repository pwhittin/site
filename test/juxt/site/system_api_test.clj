;; Copyright © 2022, JUXT LTD.

(ns juxt.site.system-api-test
  (:require
   [jsonista.core :as json]
   [juxt.site.logging :refer [with-logging]]
   [clojure.test :refer [deftest is use-fixtures testing]]
   [juxt.site.repl :as repl]
   [juxt.site.test-helpers.login :refer [with-session-token] :as login]
   [juxt.site.test-helpers.local-files-util :refer [install-resource-groups! converge!]]
   [juxt.site.test-helpers.oauth :refer [AUTH_SERVER RESOURCE_SERVER] :as oauth]
   [juxt.site.test-helpers.xt :refer [system-xt-fixture]]
   [juxt.site.test-helpers.handler :refer [*handler* handler-fixture]]
   [juxt.site.test-helpers.fixture :refer [with-fixtures]]))

;; TODO: Investigate use of test-ns-hook to run ns tests with both
;; implicit and authorization code grants. Until then, let's just use
;; 'authorization code'.

(defn bootstrap []
  (install-resource-groups! ["juxt/site/bootstrap"] AUTH_SERVER {})
  (install-resource-groups! ["juxt/site/system-api"] RESOURCE_SERVER {})

  ;; Need some test users and a way for them to authenticate
  (install-resource-groups!
   ["juxt/site/login-form" "juxt/site/example-users"]
   AUTH_SERVER
   {"session-scope" "https://auth.example.test/session-scopes/form-login-session"})

  ;; Install an authorization server
  (install-resource-groups!
   ["juxt/site/oauth-authorization-server"]
   AUTH_SERVER
   {"session-scope" "https://auth.example.test/session-scopes/form-login-session"
    "keypair" "https://auth.example.test/keypairs/test-kp-123"
    "authorization-code-length" 12
    "jti-length" 12})

  ;; TODO: Analyse the performance cost of install-resource-groups!
  ;; Perhaps optimise by only creating the installer graph once and
  ;; passing it in as a parameter.

  (converge!
   ["https://auth.example.test/clients/test-app"]
   AUTH_SERVER
   {"client-type" "public"
    "origin" "https://test-app.example.test"
    "resource-server" "https://data.example.test"
    "redirect-uris" ["https://test-app.example.test/callback"]
    "authorization-server" "https://auth.example.test"
    "scope" nil})

  (converge!
   ["https://auth.example.test/clients/read-only-app"]
   AUTH_SERVER
   {"client-type" "public"
    "origin" "https://read-only-app.example.test"
    "resource-server" "https://data.example.test"
    "redirect-uris" ["https://read-only-app.example.test/callback"]
    "authorization-server" "https://auth.example.test"
    "scope" #{"https://auth.example.test/scopes/system/read"}}))

(defn bootstrap-fixture [f]
  (bootstrap)
  (f))

(use-fixtures :once system-xt-fixture handler-fixture bootstrap-fixture)

(deftest system-api-test

  (let [session-token (login/login-with-form! "alice" "garden")
        {access-token "access_token"}
        (with-session-token
          session-token
          (oauth/implicit-authorize!
           "https://auth.example.test/oauth/authorize"
           {"client_id" "test-app"}))]

    (testing "Permissions are required for access"
      (oauth/with-bearer-token access-token
        (let [response
              (*handler*
               {:juxt.site/uri "https://data.example.test/_site/users"
                :ring.request/method :get
                :ring.request/headers
                {"accept" "application/json"}})]

          ;; This access token is not sufficient, so we get a 403
          (is (= 403 (:ring.response/status response))))))

    (converge!
     ["https://auth.example.test/role-assignments/alice-SystemReadonly"]
     AUTH_SERVER
     {})

    (testing "Access achieved with correct permissions and role assignment"
      (oauth/with-bearer-token access-token
        (let [response
              (*handler*
               {:juxt.site/uri "https://data.example.test/_site/operations"
                :ring.request/method :get
                :ring.request/headers
                {"accept" "application/json"}})]

          (is (= "application/json" (get-in response [:ring.response/headers "content-type"])))
          (is (= 200 (:ring.response/status response)))

          (let [json (some-> response :ring.response/body json/read-value)]
            (is json)
            (is (<= 8 (count json)))
            json))))))



;; So we need a system-api call that will allow us to add a
;; user. We'll still need the site tool to bootstrap users, but it
;; shouldn't be necessary to use the tool for adding new users once
;; the system has been bootstrapped.

(with-fixtures
  (let [session-token (login/login-with-form! "alice" "garden")
        {access-token "access_token"}
        (with-session-token
          session-token
          (oauth/implicit-authorize!
           "https://auth.example.test/oauth/authorize"
           {"client_id" "test-app"}))]

    (oauth/with-bearer-token access-token
      (let [response
            (*handler*
             {:juxt.site/uri "https://data.example.test/_site/users"
              :ring.request/method :post
              :ring.request/headers
              {"content-type" "application/edn"}})]

        (is (= 201 (:ring.response/status response))))))
  )

;; TODO: put-user

;; What's in put-user?
