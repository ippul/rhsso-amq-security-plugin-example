package org.apache.activemq.artemis.security.keycloak.manager;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import org.apache.activemq.artemis.core.security.CheckType;
import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.core.settings.HierarchicalRepository;
import org.apache.activemq.artemis.security.keycloak.KeycloakResourcesUtils;
import org.apache.activemq.artemis.security.keycloak.model.DestinationNameKey;
import org.apache.activemq.artemis.security.keycloak.KeycloakAuthzUtils;
import org.apache.activemq.artemis.spi.core.security.ActiveMQJAASSecurityManager;
import org.apache.activemq.artemis.utils.CompositeAddress;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class KeycloakSecurityManager extends ActiveMQJAASSecurityManager implements PropertyChangeListener {

   private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakSecurityManager.class);

   private KeycloakAuthzUtils redHatSSOCommons;

   private HierarchicalRepository<DestinationNameKey> destinationNamesResolver;

   private void init() throws IOException {
      this.redHatSSOCommons = new KeycloakAuthzUtils();
      // Subscribe current instance to receive notification about Destination configuration change
      KeycloakResourcesUtils.getInstance().addPropertyChangeListener(this);
   }

   @Override
   public boolean authorize(Subject subject, Set<Role> roles, CheckType checkType, String address) {
      if(this.destinationNamesResolver == null){
          try {
              this.destinationNamesResolver = KeycloakResourcesUtils.getInstance().getDestinationNamesResolver();
          } catch (IOException e) {
              throw new RuntimeException(e);
          }
      }
      for(KeycloakPrincipal<RefreshableKeycloakSecurityContext> curPrincipal : subject.getPrincipals(KeycloakPrincipal.class)){
         final DestinationNameKey destinationNameKey = destinationNamesResolver.getMatch(CompositeAddress.extractAddressName(address));
         return redHatSSOCommons.authorize(curPrincipal.getKeycloakSecurityContext().getTokenString(), destinationNameKey.getResourceName(), checkType.name());
      } 
      return Boolean.FALSE;
   }

   @Override
   public KeycloakSecurityManager init(Map<String, String> properties) {
      for(String key : properties.keySet()) {
         LOGGER.info("Properties key: {} value: {}", key, properties.get(key));
      }
      super.setConfigurationName(properties.get("domain"));
       try {
           this.init();
       } catch (IOException e) {
           throw new RuntimeException(e);
       }
       return this;
   }

   @Override
   public void propertyChange(PropertyChangeEvent event) {
      this.destinationNamesResolver = (HierarchicalRepository<DestinationNameKey>) event.getNewValue();
      LOGGER.info("Applied new configuration of Destination");
   }
}