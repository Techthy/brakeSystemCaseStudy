# Brake System Case Study

This repository contains a case study for managing uncertainty in a brake system using the Vitruv framework. The case study demonstrates how to annotate mechanical models with quantified uncertainty information and maintain consistency across different model representations.

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


