# Brake System Case Study

This repository contains a case study for managing uncertainty in a brake system using the Vitruv framework. The case study demonstrates how to annotate mechanical models with quantified uncertainty information and maintain consistency across different model representations.

# Running the project

In order to run the project as usual with maven you need to have the StoEx project installed in your local maven repository
You can do this by downloading the StoEx project from [here](https://github.com/Techthy/stoex) and running the following command:

```bash
  mvn clean  install
``` 

in the project directory of StoEx. Afterwards you can run the project as usual with maven.

# Models involved:"
- Brake System Model: Specification model of the brake system
- CAD Model: 3D representation of the brake system
- Uncertainty Model: Annotations of uncertainty

# File Structure
- `consistency/`: Consistency management between models using Vitruv
  - `src/main/reactions/tools/vitruv/methodologisttemplate/consistency/`: Reaction rules for model synchronization
  - `src/main/java/tools/vitruv/methodologisttemplate/consistency/`: Java helper classes for reactions
- `models/`: Example models
  - `example.mafds`: MAFDS model with uncertainty annotations
- `vsum/`: Test files
    - `src/test/java/tools/vitruv/methodologisttemplate/vsum/scenarios`: Two scenarios for brake caliper bridge gap and clamping force calculation tests 
    Additionally includes performance tests
    - `src/test/java/tools/vitruv/methodologisttemplate/vsum/uncertainty`: Utils for testing uncertainty annotations


