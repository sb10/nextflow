.. _k8s-page:

**********
Kubernetes
**********

Kubernetes is a cloud-native open-source system for deployment, scaling, and management of containerized applications.

It provides clustering and file system abstractions that allows the execution of containerised workloads across
different cloud platforms and on-premises installations.

The built-in support for Kubernetes provided by Nextflow streamlines the execution of containerised workflows in a
kubernetes cluster.

.. warning:: This is an experimental feature and it may change in a future release. It requires Nextflow
version `0.28.0` or higher.


Concepts
========

Kubernetes the main abstraction is the Pod. A Pod defines the behavior of one or more running container. 

Configuration
==============

Pipeline execution
==================

Auto-scaling
============