RAML Mock Server
================

A command-line mock server tool for RAML ([RESTful API Modeling Language](http://raml.org)) files.

Usage
=====

    java -jar raml-mock-server.jar file.raml

Where `file.raml` is in the current directory.

Features
========

* Return example / status code of first response per HTTP method
* CORS OPTIONS request and headers support
* restart on changes in the RAML file

License
=======

MIT, see LICENSE file