.. _k8s-page:

**********
Kubernetes
**********

`Kubernetes <https://kubernetes.io/>`_ is a cloud-native open-source system for deployment, scaling, and management of
containerized applications.

It provides clustering and file system abstractions that allows the execution of containerised workloads across
different cloud platforms and on-premises installations.

The built-in support for Kubernetes provided by Nextflow streamlines the execution of containerised workflows in the
Kubernetes cluster.

.. warning:: This is an experimental feature and it may change in a future release. It requires Nextflow
    version 0.28.0 or higher.


Concepts
========

Kubernetes main abstraction is the `pod`. A `pod` defines the (desired) state of one or more containers i.e. required
computing resources, storage, network configuration.

Kubernetes abstracts also the storage provisioning through the definition of one more more persistent volumes that
allow containers to access to the underlying file systems in a transparent and portable manner.

When using the ``k8s`` executor Nextflow deploys the workflow execution as a Kubernetes pod. This pod orchestrates
the workflow execution and submits a separate pod execution for each job that need to be carried out by the workflow
application.


Requirements
============

At least a `Persistent Volume <https://kubernetes.io/docs/concepts/storage/persistent-volumes/#persistent-volumes>`_ with
``ReadWriteMany`` access mode has to be defined ib the Kubernetes cluster (check the supported storage systems
at `this link <https://kubernetes.io/docs/concepts/storage/persistent-volumes/#access-modes>`_).

Such volume should be accessible through a
`Persistent Volume Claim <https://kubernetes.io/docs/concepts/storage/persistent-volumes/#persistentvolumeclaims>`_, which
will be used by Nextflow to run the application and store temporary data and the pipeline final result.

The workflow application has to be containerised using the usual Nextflow :ref:`container<process-container>` directive.


Configuration
=============

The in the ``nextflow.config`` file specify the name and the mount path of an existing persistent volume claim to be
used to deploy the workflow execution::

    k8s {
      volumeClaims {
        'your-pvc-name' {
            mountPath = '/workspace'
        }
      }
    }


As explained above, the volume claim has to refer to a persistent volume with ``ReadWriteMany`` access mode shared across
the Kubernetes cluster.

More than one volume claims can be defined repeating the name and mount path declaration in the ``volumeClaims`` block.


Execution
=========

The workflow execution needs to be submitted from a computer able to connect to the Kubernetes cluster.

Nextflow uses the Kubernetes configuration file available at the path ``$HOME/.kube/config`` or the file specified
by the environment variable ``KUBECONFIG``.

You can verify such configuration with the command below::

    $ kubectl cluster-info
    Kubernetes master is running at https://your-host:6443
    KubeDNS is running at https://your-host:6443/api/v1/namespaces/kube-system/services/kube-dns:dns/proxy


The pipeline execution is submitted using the usual nextflow run command adding the option ``-with-k8s`` as shown below::

    nextflow run nextflow-io/rnaseq-nf -with-k8s


This will create and execute a pod running the main workflow application. Once the pod starts the application in the
foreground just prints the console output produced by that pod.


Limitations
===========

In most common deployment scenario the Kubernetes cluster manages its own storage which is not directly
shared with the hosting environment. For this reason Nextflow only allows the execution of workflow applications
published in GitHub.

Advanced configuration
======================

Read :ref:`Kubernetes configuration<config-k8s>` section to learn more about advanced cloud configuration options.