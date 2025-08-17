# CloudNet Rest Module

![Build](https://github.com/CloudNetService/module-rest/actions/workflows/ci.yml/badge.svg)
![Release](https://img.shields.io/github/v/release/CloudNetService/module-rest?sort=date&logo=github&include_prereleases)

## What is the purpose of this module?

This CloudNet v4 module allows to get and control certain components of a cluster via a rest interface (for example
services, tasks, groups, templates, service/node logs, etcetera).

## Installation

The module can be installed from the console of your running ClouNet installation using `modules install CloudNet-Rest`.
You can also download the jar from [the latest release](https://github.com/CloudNetService/module-rest/releases) and
drop it into the `modules` folder of your node installation. The latest nightly build (based on the current main branch)
can be downloaded [here](https://nightly.link/CloudNetService/module-rest/workflows/ci/main/CloudNet-Rest.zip).

## Documentation

An openapi spec is bundled in the final jar and visualized at `http://<your-rest-endpoint>/api/v3/documentation` (the
spec can be downloaded from there too). A permanent visualization of the latest documentation can be
found [here](https://redocly.github.io/redoc/?url=https://raw.githubusercontent.com/CloudNetService/module-rest/refs/heads/main/cloudnet-rest-module/src/main/resources/documentation/swagger.yaml&nocors).

## Links

- [Support Discord](https://discord.cloudnetservice.eu)
- [Main CloudNet Repository](https://github.com/CloudNetService/CloudNet)
- [Issue Tracker](https://github.com/CloudNetService/module-rest/issues)
- [Latest Release](https://github.com/CloudNetService/module-rest/releases/latest)

## Compile from source

To compile this project you need JDK 21 and an internet connection. Then clone this repository and run `./gradlew build`
inside the cloned project.
