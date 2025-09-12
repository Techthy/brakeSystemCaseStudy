# Add external ecore model reference


This document describes how to add an external ecore model as a dependency to this ecore model. As a prerequisite the external ecore model should be available as a maven dependency and already added to the pom.xml of your project.


1. In the ecore file itself add a reference to the external ecore model. This will be under `plattform:/plugin/` if the ecore model is in a plugin/other maven project.
As an example this could look something like this:

   ```xml
   <eClassifiers xsi:type="ecore:EReference" name="expression" eType="ecore:EClass  platform:/plugin/tools.vitruv.stoex/model/generated/Stoex.ecore#//Expression"
       containment="true"/>
   ```

2. In order to make the ecore model and genmodel available during code generation, you need to add a uriMap entry to the workflow file (generate.mwe2). This will map the platform uri to the resource uri. Again the external ecore model should be under `platform:/plugin/...` and we want it to be available at `platform:/resource/...`. You need do this for the ecore and genmodel! An example could look like this:

   ```java
   uriMap = {
       from = "platform:/plugin/tools.vitruv.stoex/model/generated/Stoex.ecore"
       to = "platform:/resource/tools.vitruv.stoex/model/generated/Stoex.ecore"
   }
   ```

3. Either add the model to the plugin.xml of your project to generate the code for the model. This is necessary if you are not generating the code in the other project/plugin. An example could look like this:

   ```xml
   <plugin>
       <extension
           point="org.eclipse.emf.ecore.generated_package">
           <package
               uri="platform:/plugin/tools.vitruv.stoex/model/generated/Stoex.ecore"
               class="tools.vitruv.stoex.model.generated.GeneratedPackageImpl"
               genModel="platform:/plugin/tools.vitruv.stoex/model/generated/Stoex.genmodel" />
       </extension>
   </plugin>
   ```
    Or if you are already generating the model code in the other project you need to modify the genmodel of the ecore referencing the external ecore model. You need to add the external ecore model as a referenced genmodel. This can be done adding the following to the genmodel file:

    ```xml
    <genModel ...>
        <usedGenPackages="platform:/plugin/tools.vitruv.stoex/model/generated/Stoex.genmodel#//stoex">
    </genModel>
    ```
