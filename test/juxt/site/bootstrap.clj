;; Copyright © 2022, JUXT LTD.

(ns juxt.site.bootstrap
  (:require
   [juxt.site.init :refer [put! base-uri substitute-actual-base-uri converge!]]
   [juxt.site.actions :as actions]
   [juxt.site.resources.openid :as openid]
   [juxt.site.resources.session-scope :as session-scope]
   [juxt.site.resources.user :as user]))

(defn install-system-subject! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.init/put!
      ;; tag::install-system-subject![]
      {:xt/id "https://example.org/subjects/system"
       :juxt.site/description "The system subject"
       :juxt.site/type "https://meta.juxt.site/site/subject"}
      ;; end::install-system-subject![]
      )))))

(defn install-system-permissions! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.init/put!
      ;; tag::install-system-permissions![]
      {:xt/id "https://example.org/permissions/system/bootstrap"
       :juxt.site/type "https://meta.juxt.site/site/permission" ; <1>
       :juxt.site/action #{"https://example.org/actions/create-action"
                                 "https://example.org/actions/grant-permission"} ; <2>
       :juxt.site/purpose nil      ; <3>
       :juxt.site/subject "https://example.org/subjects/system" ; <4>
       }
      ;; end::install-system-permissions![]
      )))))

(defn install-do-action-fn! [_]
  (put! (actions/install-do-action-fn (base-uri))))

(defn grant-permission-get-not-found! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (do
       (juxt.site.init/do-action
        "https://example.org/subjects/system"
        "https://example.org/actions/grant-permission"
        {:xt/id "https://example.org/permissions/get-not-found"
         :juxt.site/action "https://example.org/actions/get-not-found"
         :juxt.site/purpose nil}))))))

(defn install-create-action! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.init/put!
      {:xt/id "https://example.org/actions/create-action"
       :juxt.site/description "The action to create all other actions"
       :juxt.site/type "https://meta.juxt.site/site/action"

       :juxt.site/rules
       '[
         ;; Creating actions should only be available to the most trusted
         ;; subjects. Actions can write directly to the database, if they wish.
         [(allowed? subject resource permission)
          [permission :juxt.site/subject subject]]]

       :juxt.site.malli/input-schema
       [:map
        [:xt/id [:re "https://example.org/actions/(.+)"]]
        [:juxt.site/rules [:vector [:vector :any]]]]

       :juxt.site/prepare
       {:juxt.site.sci/program
        (pr-str
         '(let [content-type (-> *ctx*
                                 :juxt.site/received-representation
                                 :juxt.http/content-type)
                body (-> *ctx*
                         :juxt.site/received-representation
                         :juxt.http/body)]
            (case content-type
              "application/edn"
              (some->
               body
               (String.)
               clojure.edn/read-string
               juxt.site.malli/validate-input
               (assoc :juxt.site/type "https://meta.juxt.site/site/action")))))}

       :juxt.site/transact
       {:juxt.site.sci/program
        (pr-str
         '[[:xtdb.api/put *prepare*]])}})))))

(defn create-grant-permission-action! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-grant-permission-action![]
     (juxt.site.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/grant-permission"
       :juxt.site/type "https://meta.juxt.site/site/action"

       :juxt.site.malli/input-schema
       [:map
        [:xt/id [:re "https://example.org/permissions/(.+)"]]
        [:juxt.site/action [:re "https://example.org/actions/(.+)"]]
        [:juxt.site/purpose [:maybe :string]]]

       :juxt.site/prepare
       {:juxt.site.sci/program
        (pr-str
         '(let [content-type (-> *ctx*
                                 :juxt.site/received-representation
                                 :juxt.http/content-type)
                body (-> *ctx*
                         :juxt.site/received-representation
                         :juxt.http/body)]
            (case content-type
              "application/edn"
              (some->
               body
               (String.)
               clojure.edn/read-string
               juxt.site.malli/validate-input
               (assoc
                :juxt.site/type "https://meta.juxt.site/site/permission")))))}

       :juxt.site/transact
       {:juxt.site.sci/program
        (pr-str
         '[[:xtdb.api/put *prepare*]])}

       :juxt.site/rules
       '[
         [(allowed? subject resource permission)
          [permission :juxt.site/subject subject]]

         ;; This might be overly powerful, as a general way of granting anyone a
         ;; permission on any action! Let's comment for now
         #_[(allowed? subject resource permission)
            [subject :juxt.site/user-identity id]
            [id :juxt.site/user user]
            [user :role role]
            [permission :role role]]]})
     ;; end::create-grant-permission-action![]
     ))))

(defn create-action-get-not-found! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/get-not-found"
       #_:juxt.site/transact
       #_{:juxt.site.sci/program
        (pr-str
         '(do
            [[:ring.response/status 404]]))}
       :juxt.site/rules
       [
        ['(allowed? subject resource permission)
         ['permission :xt/id]]]})))))

(defn create-action-install-not-found! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/install-not-found"

       :juxt.site.malli/input-schema
       [:map
        [:xt/id [:re "https://example.org/.*"]]]

       :juxt.site/prepare
       {:juxt.site.sci/program
        (pr-str
         '(let [content-type (-> *ctx*
                                 :juxt.site/received-representation
                                 :juxt.http/content-type)
                body (-> *ctx*
                         :juxt.site/received-representation
                         :juxt.http/body)]
            (case content-type
              "application/edn"
              (some->
               body
               (String.)
               clojure.edn/read-string
               juxt.site.malli/validate-input
               (assoc
                :juxt.site/type "https://meta.juxt.site/not-found"
                :juxt.site/methods
                {:get {:juxt.site/actions #{"https://example.org/actions/get-not-found"}}
                 :head {:juxt.site/actions #{"https://example.org/actions/get-not-found"}}})))))}

       :juxt.site/transact
       {:juxt.site.sci/program
        (pr-str
         '[[:xtdb.api/put *prepare*]])}

       :juxt.site/rules
       [
        ['(allowed? subject resource permission)
         '[permission :juxt.site/subject subject]]]})))))

(defn grant-permission-install-not-found! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/system/install-not-found"
       :juxt.site/subject "https://example.org/subjects/system"
       :juxt.site/action "https://example.org/actions/install-not-found"
       :juxt.site/purpose nil})))))

(defn install-not-found-resource! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (do
       (juxt.site.init/do-action
        "https://example.org/subjects/system"
        "https://example.org/actions/install-not-found"
        {:xt/id "https://example.org/_site/not-found"}))))))

;; TODO: In the context of an application, rename 'put' to 'register'
(defn create-action-register-application!
  "Install an action to register an application"
  [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/register-application"

       :juxt.site.malli/input-schema
       [:map
        [:juxt.site/client-id [:re "[a-z-]{3,}"]]
        [:juxt.site/redirect-uri [:re "https://"]]]

       :juxt.site/prepare
       {:juxt.site.sci/program
        (pr-str
         '(let [content-type (-> *ctx*
                                 :juxt.site/received-representation
                                 :juxt.http/content-type)
                body (-> *ctx*
                         :juxt.site/received-representation
                         :juxt.http/body)]
            (case content-type
              "application/edn"
              (let [input (some->
                           body
                           (String.)
                           clojure.edn/read-string
                           juxt.site.malli/validate-input)
                    client-id (:juxt.site/client-id input)]
                (into
                 {:xt/id (format "%s/applications/%s" (:juxt.site/base-uri *ctx*) client-id)
                  :juxt.site/type "https://meta.juxt.site/site/application"}
                 input)))))}

       :juxt.site/transact
       {:juxt.site.sci/program
        (pr-str
         '[[:xtdb.api/put *prepare*]])}

       :juxt.site/rules
       '[[(allowed? subject resource permission)
          [permission :juxt.site/subject "https://example.org/subjects/system"]]

         [(allowed? subject resource permission)
          [id :juxt.site/user user]
          [subject :juxt.site/user-identity id]
          [user :role role]
          [permission :role role]]]})))))

(defn grant-permission-to-invoke-action-register-application! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/system/register-application"
       :juxt.site/subject "https://example.org/subjects/system"
       :juxt.site/action "https://example.org/actions/register-application"
       :juxt.site/purpose nil})))))

(def dependency-graph
  {"https://example.org/_site/do-action"
   {:create install-do-action-fn!}

   "https://example.org/subjects/system"
   {:create install-system-subject!}

   "https://example.org/permissions/system/bootstrap"
   {:create install-system-permissions!}

   "https://example.org/permissions/get-not-found"
   {:create grant-permission-get-not-found!}

   "https://example.org/actions/create-action"
   {:create install-create-action!}

   "https://example.org/actions/grant-permission"
   {:create create-grant-permission-action!
    :deps #{"https://example.org/_site/do-action"
            "https://example.org/subjects/system"
            "https://example.org/actions/create-action"
            "https://example.org/permissions/system/bootstrap"}}

   "https://example.org/actions/get-not-found"
   {:create create-action-get-not-found!
    :deps #{"https://example.org/_site/do-action"
            "https://example.org/subjects/system"
            "https://example.org/actions/create-action"}}

   "https://example.org/actions/install-not-found"
   {:create create-action-install-not-found!
    :deps #{"https://example.org/_site/do-action"
            "https://example.org/subjects/system"
            "https://example.org/actions/create-action"}}

   "https://example.org/permissions/system/install-not-found"
   {:create grant-permission-install-not-found!
    :deps #{"https://example.org/_site/do-action"
            "https://example.org/subjects/system"
            "https://example.org/actions/grant-permission"}}

   "https://example.org/_site/not-found"
   {:create install-not-found-resource!
    :deps #{"https://example.org/_site/do-action"
            "https://example.org/subjects/system"
            "https://example.org/actions/install-not-found"}}

   :juxt.site.init/system
   {:deps #{"https://example.org/_site/do-action"
            "https://example.org/_site/not-found"

            "https://example.org/subjects/system"

            "https://example.org/actions/create-action"
            "https://example.org/actions/grant-permission"
            "https://example.org/actions/install-not-found"
            "https://example.org/actions/get-not-found"

            "https://example.org/permissions/system/bootstrap"
            "https://example.org/permissions/system/install-not-found"
            "https://example.org/permissions/get-not-found"}}

   "https://example.org/actions/register-application"
   {:create create-action-register-application!
    :deps #{:juxt.site.init/system}}

   "https://example.org/permissions/system/register-application"
   {:create grant-permission-to-invoke-action-register-application!
    :deps #{:juxt.site.init/system}}})

(defn bootstrap* [opts]
  (converge!
   #{:juxt.site.init/system

     "https://example.org/actions/put-user"
     ;;"https://example.org/actions/put-openid-user-identity"
     "https://example.org/permissions/system/put-user"
     ;;"https://example.org/permissions/system/put-openid-user-identity"

     :juxt.site.openid/all-actions
     :juxt.site.openid/default-permissions
     (substitute-actual-base-uri "https://example.org/permissions/system/put-session-scope")}
   (substitute-actual-base-uri
    (merge
     dependency-graph
     openid/dependency-graph
     session-scope/dependency-graph
     user/dependency-graph))
   opts))

(defn bootstrap []
  (bootstrap* {:dry-run? true :recreate? false}))

(defn bootstrap! []
  (bootstrap* {:dry-run? false :recreate? false}))

(defn bootstrap!! []
  (bootstrap* {:dry-run? false :recreate? true}))

;; Install openid connect provider, must be in .config/site/openid-client.edn
;; Or rather, POST a document to the https://example.org/actions/install-openid-issuer
;; and https://example.org/actions/install-openid-client
;; and https://example.org/actions/install-openid-login-endpoint

;; TODO: Rewrite this in terms of a converge
(defn install-openid! []
  (converge!
   #{:juxt.site.init/system
     :juxt.site.openid/all-actions
     :juxt.site.openid/default-permissions
     (substitute-actual-base-uri "https://example.org/openid/auth0/issuer")
     (substitute-actual-base-uri "https://example.org/openid/auth0/client-configuration")
     (substitute-actual-base-uri "https://example.org/openid/login")
     ;;(substitute-actual-base-uri "https://example.org/openid/callback")
     (substitute-actual-base-uri "https://example.org/session-scopes/openid")}

   (substitute-actual-base-uri
    (merge
     dependency-graph
     openid/dependency-graph
     session-scope/dependency-graph
     user/dependency-graph))

   {:dry-run? false :recreate? true}))

(defn install-openid-user!
  [& {:keys [name username]
      :juxt.site.jwt.claims/keys [iss sub nickname]}]
  (user/put-user! :username username :name name)
  (openid/put-openid-user-identity!
   (cond-> {:username username
            :juxt.site.jwt.claims/iss iss}
     sub (assoc :juxt.site.jwt.claims/sub sub)
     nickname (assoc :juxt.site.jwt.claims/nickname nickname))))

(comment
  (install-openid-user!
   :username "mal"
   :name "Malcolm Sparks"
   :juxt.site.jwt.claims/iss "https://juxt.eu.auth0.com/"
   :juxt.site.jwt.claims/nickname "malcolmsparks"))

(comment
  (put!
   {:xt/id "https://site.test/actions/whoami"
    :juxt.site/type "https://meta.juxt.site/site/action"
    :juxt.site/rules
    '[
      [(allowed? subject resource permission)
       [permission :juxt.site/subject subject]]

      [(allowed? subject resource permission)
       [subject :juxt.site/user-identity id]
       [id :juxt.site/user user]
       [permission :juxt.site/user user]]]}))

(comment
  (put!
   {:xt/id "https://site.test/whoami"
    :juxt.site/methods
    {:get {:juxt.site/actions #{"https://site.test/actions/whoami"}}}
    :juxt.http/content-type "text/plain"
    :juxt.http/content "Hello - who are you?"}))

(comment
  (put!
   {:xt/id "https://site.test/permissions/malcolm/whoami"
    :juxt.site/type "https://meta.juxt.site/site/permission"
    :juxt.site/user "https://site.test/users/mal"
    :juxt.site/action "https://site.test/actions/whoami"
    :juxt.site/purpose nil}))
