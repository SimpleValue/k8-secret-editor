# k8-secret-editor

Editing Kubernetes secrets is
[cumbersome](https://dev.to/focusedlabs/editing-kubernetes-secrets-inline-44f7). Mainly
due to the fact that all text values are also encoded as base64. This
lib provides a collection of utils to edit kubernetes secrets
programmatically or via a Clojure REPL.

## License

EPL 2.0
