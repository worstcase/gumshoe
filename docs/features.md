Gumshoe Feature Roadmap

Current version:

* Gumshoe captures and displays socket usage information.

Partial support:

* The probe also can collect and log information about unclosed sockets
  but the viewer does not yet use this information.

* The hook is also capable of capturing file usage,
  but the probe and viewer do not make use of this.

Expected soon:

* Capturing all threads at intervals instead of just those involved in socket I/O
  would result in the familiar flame graphs or root graphs used for
  processor utilization analysis.
