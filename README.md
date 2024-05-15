## Configurations

### Branch features/upgrades/amq-operator/7.11.6/broker/7.10.2
| Component      | Name             | Version                           |
|----------------|------------------|-----------------------------------|
| RHSSO operator | rhsso-operator   | rhsso-operator.7.6.8-opr-001      |
| AMQ operator   | amq-broker-rhel8 | amq-broker-operator.v7.11.6-opr-2 |
| AMQ broker     | amq-broker       | 7.10.2                            |

## Install
```
oc extract secret/router-certs-default -n openshift-ingress --keys=tls.crt --to=/tmp --confirm
keytool -import -noprompt -file /tmp/tls.crt -alias .apps-crc.testing -keystore ./helm/rhsso-amq-security-plugin-example/truststore/truststore.jks -storepass password
helm repo add redhat-cop https://redhat-cop.github.io/helm-charts
helm --debug upgrade --install -f ./helm/operators-values.yaml example-operators redhat-cop/operators-installer --namespace=example --create-namespace
helm --debug upgrade --install rhsso-amq-security-plugin-example ./helm/rhsso-amq-security-plugin-example/ --namespace example --create-namespace
```

## Check installation

```
oc project example
helm list 

NAME                                NAMESPACE   REVISION    UPDATED                                 STATUS      CHART
example-operators                   example     2           2024-05-15 16:44:23.213470039 +0100 BST deployed    operators-installer-2.4.3
rhsso-amq-security-plugin-example   example     1           2024-05-15 16:46:20.484940498 +0100 BST deployed    rhsso-amq-security-plugin-example-1.0.0
```