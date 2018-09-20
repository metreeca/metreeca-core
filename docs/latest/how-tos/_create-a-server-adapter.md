---
title: 		How To Create a Server Adapter
excerpt:	Usability guidelines and other configuration tips
caption:    "${module.caption}"
project:    "${module.project}"
version:    "${module.version}"
---

- the framework
	- expects a wrapping server to hande HTTP exchanges with the server
	- is agnostic about the implementation of the wrapping server and doesn't expect to be interfaced over a specific API (e.g. Servlets)
	- requires a server adapter to handle and convert HTTP requests and responses
	
- create a share tool manager (Vault)
- customize tool
	- e.g to replace the system resource Loader
- create a server using tray.get(Server.Tool)
- convert incoming HTTP requests to link.Request objects
	- populate root, target, user, headers, body, … as required
- create an empty link.Response object
- process the Request/Response pair
- convert Response to an outgoing HTTP response
	- read from status, headers, body, … as required
- handle 0 response status
	- the linked data pipeline has no definition for the requested target resource
	- the wrapping server has a chance to forward the request to other com.metreeca.rest.handlers (e.g. servlets)
	- if unable/unwilling to forward must generate an appropriate response (e.g. 404 or 501)
	
			 When a request method is received that is unrecognized or not implemented by an origin server, the origin server SHOULD respond with the 501 (Not Implemented) status code.  When a request method is received that is known by an origin server but not allowed for the target resource, the origin server SHOULD respond with the 405 (Method Not Allowed) status code.

