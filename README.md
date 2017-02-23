# Github Release Watcher

> A simple Scala.js app to list releases in for one or more projects in Github.

See it in action [here](https://releasewatcher-49645.firebaseapp.com/).

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
