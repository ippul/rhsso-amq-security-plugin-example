# Branch features/upgrades/amq-operator/7.11.6/broker/7.10.2

## Current branch configuration
| Component      | Name             | Version                           |
|----------------|------------------|-----------------------------------|
| RHSSO operator | rhsso-operator   | rhsso-operator.7.6.8-opr-001      |
| AMQ operator   | amq-broker-rhel8 | amq-broker-operator.v7.11.6-opr-2 |
| AMQ broker     | amq-broker       | 7.11.6(init image 7.11.6-3)       |

## Install operators
```
helm repo add redhat-cop https://redhat-cop.github.io/helm-charts
helm --debug upgrade --install -f ./helm/operators-values-rhamq.yaml example-operators redhat-cop/operators-installer
helm --debug upgrade --install -f ./helm/operators-values-rhsso.yaml example-operators redhat-cop/operators-installer --namespace=example-security-plugin --create-namespace
helm --debug upgrade --install -f ./helm/operators-values-rhsso.yaml example-operators redhat-cop/operators-installer --namespace=example-plain-integration --create-namespace
```

## Create the truststores
```
oc extract secret/router-certs-default -n openshift-ingress --keys=tls.crt --to=/tmp --confirm
rm helm/rhsso-amq-plain-example/truststore/truststore.jks
rm helm/rhsso-amq-security-plugin-example/truststore/truststore.jks
keytool -import -noprompt -file /tmp/tls.crt -alias .apps-crc.testing -keystore ./helm/rhsso-amq-plain-example/truststore/truststore.jks -storepass password
keytool -import -noprompt -file /tmp/tls.crt -alias .apps-crc.testing -keystore ./helm/rhsso-amq-security-plugin-example/truststore/truststore.jks -storepass password
```

## Install plain integration 
```
helm --debug upgrade --install rhsso-amq-plain-example ./helm/rhsso-amq-plain-example/ --namespace example-plain-integration --create-namespace
```

## Install with security plugin
```
helm --debug upgrade --install rhsso-amq-security-plugin-example ./helm/rhsso-amq-security-plugin-example/ --namespace example-security-plugin --create-namespace
```