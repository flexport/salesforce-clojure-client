(ns salesforce.core-test
  (:use clojure.test
        clj-http.fake
        salesforce.core))

(defmacro with-private-fns [[ns fns] & tests]
  "Refers private fns from ns and runs tests in context."
  `(let ~(reduce #(conj %1 %2 `(ns-resolve '~ns '~%2)) [] fns)
     ~@tests))

(deftest with-version-test
  (testing "should set +version+ to a given string"
    (is (= "26.0" (with-version "26.0" @+version+)))))

(def well-formed-auth-app-data
  {:client-id      "my-ID"
   :client-secret  "my-SECRET"
   :username       "my-USERNAME"
   :password       "my-PASSWORD"
   :security-token "my-TOKEN"})

(def well-formed-auth-token-mock
  {:instance_url "https://salesforce.localhost" :access_token "my_token"})

(def expected-auth-header-from-well-formed-auth-token-mock
  {"Authorization" "Bearer my_token"})

(deftest test-auth-prepare
  (testing "should generate expected params with default url when login-url not provided"
    (is (= {:method :post
            :url "https://login.salesforce.com/services/oauth2/token"
            :form-params {:grant_type    "password"
                          :client_id     "my-ID"
                          :client_secret "my-SECRET"
                          :format        "json",
                          :username      "my-USERNAME"
                          :password      "my-PASSWORDmy-TOKEN"}}
           (auth-prepare well-formed-auth-app-data))))

  (testing "should generate expected params with given login-url when provided"
    (let [params-with-login-host (merge well-formed-auth-app-data {:login-host "salesforce.localhost"})]
      (is (= {:method :post
              :url "https://salesforce.localhost/services/oauth2/token"
              :form-params {:grant_type    "password"
                            :client_id     "my-ID"
                            :client_secret "my-SECRET"
                            :format        "json",
                            :username      "my-USERNAME"
                            :password      "my-PASSWORDmy-TOKEN"}}
             (auth-prepare params-with-login-host)))))

  (testing "should generate expected params when given http client config"
    (let [sf-params-with-login-host (merge well-formed-auth-app-data {:login-host "salesforce.localhost"})
          http-client-params {:connection-timeout 10 :connection-request-timeout 15}]
      (is (= {:method :post
              :url "https://salesforce.localhost/services/oauth2/token"
              :connection-timeout         10
              :connection-request-timeout 15
              :form-params {:grant_type    "password"
                            :client_id     "my-ID"
                            :client_secret "my-SECRET"
                            :format        "json",
                            :username      "my-USERNAME"
                            :password      "my-PASSWORDmy-TOKEN"}}
             (auth-prepare sf-params-with-login-host http-client-params))))))

(deftest test-soql-prepare
  (testing "should generate expected params "
    (is (= {:method :get
            :headers expected-auth-header-from-well-formed-auth-token-mock
            :url "https://salesforce.localhost/services/data/v39.0/query?q=SELECT+foo+from+Account"}
           (soql-prepare "SELECT foo from Account" well-formed-auth-token-mock)))))

(deftest test-soql!
  (testing "response-shape"
    (is (= {:api-result {:a 1} :limit-info {:used 100 :available 555}}
           ; Note that if the none of the urls we give in a `with-fake-routes` block match what is given to the client, then the response is NOT mocked
           ; and the client makes an actual call. Because we use host salesforce.localhost in the test, the request won't go anywhere if tht happens.
           (with-fake-routes
             {"https://salesforce.localhost/services/data/v39.0/query?q=SELECT+foo+from+Account" (fn [request] {:status 200 :headers {"Sforce-Limit-Info" "api-usage=100/555"} :body "{\"a\":1}"})}
             (soql! "SELECT foo from Account" well-formed-auth-token-mock))))))

;; Private functions


(with-private-fns [salesforce.core [gen-query-url]]
  (deftest gen-query-url-test
    (testing "should generate a valid url for salesforce.com"
      (let [url (gen-query-url "20.0" "SELECT name from Account")]
        (is (= url "/services/data/v20.0/query?q=SELECT+name+from+Account"))))))

(with-private-fns [salesforce.core [prepare-request]]
  (deftest test-prepare-request
    (testing "should generate expected params when no client params specified"
      (is (= {:method :get
              :url "https://salesforce.localhost/foo/bar/baz"
              :headers expected-auth-header-from-well-formed-auth-token-mock}
             (prepare-request :get "/foo/bar/baz" well-formed-auth-token-mock)))))

  (testing "should generate expected params when client params are specified"
    (is (= {:method :get
            :url "https://salesforce.localhost/foo/bar/baz"
            :headers expected-auth-header-from-well-formed-auth-token-mock
            :connection-timeout 5000}
           (prepare-request :get "/foo/bar/baz" well-formed-auth-token-mock {:connection-timeout 5000})))))

(with-private-fns [salesforce.core [parse-limit-info]]
  (deftest test-parse-limit-info
    (testing "extracts info"
      (is (= {:used 289725 :available 645000}
             (parse-limit-info "api-usage=289725/645000"))))))
