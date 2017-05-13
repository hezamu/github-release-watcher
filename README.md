# Github Release Watcher

> A simple Scala.js app to list releases in for one or more projects in Github.

You need authenticate with your Github account to see the releases. See it in action at [vaadin-release-watcher.firebaseapp.com](https://vaadin-release-watcher.firebaseapp.com/). 

## Build Setup

``` bash
# Start SBT
$ sbt

# Start a loop that builds the project whenever sources change
sbt> ~fastOptJS
```

You also need to serve the project directory over HTTP to access it with your browser. Eg. with Python:
```
$ python -c "import BaseHTTPServer as bhs, SimpleHTTPServer as shs; bhs.HTTPServer((\"127.0.0.1\", 8888), shs.SimpleHTTPRequestHandler).serve_forever()"
```
After this the UI is accessible at [http://localhost:8888/index-dev.html](http://localhost:8888/index-dev.html)
