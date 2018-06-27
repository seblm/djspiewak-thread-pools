[![Build Status](https://travis-ci.org/seblm/djspiewak-thread-pools.svg?branch=master)](https://travis-ci.org/seblm/djspiewak-thread-pools)

Source code to understand [Thread Pools](https://gist.github.com/djspiewak/46b543800958cf61af6efa8e072bfd5c).

```bash
$ curl http://localhost:8080/threadpools & sleep 0.04 && \
  curl http://localhost:8080/threadpools & sleep 0.04 && \
  curl http://localhost:8080/threadpools & sleep 1 && \
  curl http://localhost:8080/generate
```
