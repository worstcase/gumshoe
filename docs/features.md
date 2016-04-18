Gumshoe Feature Roadmap
=======================

Current version
---------------

- Flame graph and root graph
- Live capture and visualize or record to text file and view later 
- Capture socket I/O information
- Capture unclosed socket information

Partial support
---------------

- The hook is also capable of capturing file usage,
  but the probe and viewer do not make use of this.

Expected soon
-------------

- Capturing all threads at intervals instead of just those involved in socket I/O
  would result in the familiar flame graphs or root graphs used for
  processor utilization analysis.
