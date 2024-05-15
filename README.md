# Install
```
oc extract secret/router-certs-default -n openshift-ingress --keys=tls.crt --to=/tmp --confirm
keytool -import -noprompt -file /tmp/tls.crt -alias .apps-crc.testing -keystore ./helm/rhsso-amq-security-plugin-example/truststore/truststore.jks -storepass password
helm repo add redhat-cop https://redhat-cop.github.io/helm-charts
helm --debug upgrade --install -f ./helm/operators-values.yaml example-operators redhat-cop/operators-installer --namespace=example --create-namespace
helm --debug upgrade --install rhsso-amq-security-plugin-example ./helm/rhsso-amq-security-plugin-example/ --namespace example --create-namespace
```

