[![build](https://github.com/zyclonite/nassh-relay/actions/workflows/build.yml/badge.svg)](https://github.com/zyclonite/nassh-relay/actions/workflows/build.yml)
[![Quay.io Enabled](https://badgen.net/badge/quay%20pulls/enabled/green)](https://quay.io/repository/zyclonite/nassh-relay)
[![Docker Pulls](https://badgen.net/docker/pulls/zyclonite/nassh-relay)](https://hub.docker.com/r/zyclonite/nassh-relay)

# nassh-relay

Relay Server for the Secure Shell Chromium plugin

## Documentation

See https://github.com/zyclonite/nassh-relay/wiki/Manual for details

## Docker

```
$ docker run -d --name nassh-relay -p 8022:8022 zyclonite/nassh-relay
```

## Demo

[![PlayWithDocker](https://github.com/play-with-docker/stacks/raw/cff22438cb4195ace27f9b15784bbb497047afa7/assets/images/button.png)](http://play-with-docker.com?stack=https://gist.githubusercontent.com/zyclonite/2351c74e618382486855683b8ff749e6/raw/182bddf7614024e8c4230522de9ab9dfa798ef86/nassh-stack.yml)

client options (fill in your temp docker url from pwd)
```
--proxy-host=pwd10-XXX-XXX-XXX-8022.host1.labs.play-with-docker.com --proxy-port=80
```

## Source

The project's source code is hosted at:

https://github.com/zyclonite/nassh-relay

## Github procedures

nassh-relay accepts contributions through pull requests on GitHub. After review a pull
request should either get merged or be rejected.

When a pull request needs to be reworked, say you have missed something, the pull
request is then closed, at the time you finished the required changes you should
reopen your original Pull Request and it will then be re-evaluated. At that point if
the request is aproved we will then merge it.

Make sure you always rebase your branch on master before submitting pull requests.
