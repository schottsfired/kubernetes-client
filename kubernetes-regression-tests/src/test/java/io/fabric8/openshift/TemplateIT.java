package io.fabric8.openshift;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.openshift.api.model.Parameter;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.api.model.TemplateBuilder;
import io.fabric8.openshift.api.model.TemplateList;
import io.fabric8.openshift.client.OpenShiftClient;
import org.arquillian.cube.kubernetes.api.Session;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

import static io.fabric8.kubernetes.client.utils.ReplaceValueStream.replaceValues;
import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(ArquillianConditionalRunner.class)
@RequiresOpenshift
public class TemplateIT {
  @ArquillianResource
  OpenShiftClient client;

  @ArquillianResource
  Session session;

  @Test
  public void testLoad() {
    String currentNamespace = session.getNamespace();
    Template template = client.templates().inNamespace(currentNamespace).load(replaceValues(
      getClass().getResourceAsStream("/test-template.yml"), Collections.singletonMap("REDIS_PASSWORD", "secret"))
    ).get();
    assertThat(template).isNotNull();
    assertEquals(1, template.getObjects().size());
  }

  @Test
  public void testCrud() {
    String currentNamespace = session.getNamespace();
    Service aService = new ServiceBuilder()
      .withNewMetadata().withName("bar").endMetadata()
      .withNewSpec()
      .addNewPort()
      .withPort(80).endPort()
      .addToSelector("cheese", "edam")
      .withType("ExternalName")
      .endSpec()
      .build();

    Template template1 = new TemplateBuilder()
      .withNewMetadata().withName("foo").endMetadata()
      .addToObjects(aService)
      .build();

    client.templates().inNamespace(currentNamespace).create(template1);

    TemplateList aList = client.templates().inNamespace(currentNamespace).list();
    assertThat(aList).isNotNull();
    assertEquals(1, aList.getItems().size());

    boolean bDeleted = client.templates().inNamespace(currentNamespace).withName("foo").delete();
    assertTrue(bDeleted);
  }
}
