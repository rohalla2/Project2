#!/usr/bin/python

import sys
import time
import urllib2
import os
from urllib2 import urlopen,HTTPError

TIMEOUT = 5
NORMAL_FILES = ['/index.html', '/foo/bar.html', '/test.pdf', '/test.txt', '/test.png', '/test.jpg', '/tmp.txt']
REDIRECTS = [['/cats', 'http://en.wikipedia.org/wiki/Cat']]
NOTFOUNDS = ['/redirect.defs', '/not/a/real/url.html', '/']

# prevents us from following redirects, so we can get the real response code
class DontFollowHttpRedirectHandler(urllib2.HTTPRedirectHandler):
	def http_error_307(self, req, fp, code, msg, headers):
		raise urllib2.HTTPError(req.get_full_url(), code, msg, headers, fp)
	http_error_302 = http_error_303 = http_error_307 #= http_error_301

class bcolors:
    HEADER = '\033[95m'
    OKBLUE = '\033[94m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'
    UNDERLINE = '\033[4m'
    test_fail = FAIL + 'FAIL'  + ENDC
    test_pass = OKGREEN + 'PASS' + ENDC

def build_url(host, port, filename):
	return 'https://' + host + ':' + port + filename

# returns request, response, code
# response is None for 4xx or 5xx response
def maybe_fetch(host, port, filename, op='GET'):
	request = urllib2.Request(build_url(host, port, filename))
	request.get_method = lambda : op
	opener = urllib2.build_opener(DontFollowHttpRedirectHandler)
	response = None
	try:
		response = opener.open(request)
	except HTTPError as e:
		return request, None, e.code
	return request, response, response.code

def test_POST(host, port):
	print 'test POST (unsupported)'
	for filename in NORMAL_FILES:
		request, response, code = maybe_fetch(host, port, filename, op='POST')
		if code != 403:
			return bcolors.test_fail
	return bcolors.test_pass

def test_INVALID(host, port):
	print 'test INVALID'
	for filename in NORMAL_FILES:
		request, response, code = maybe_fetch(host, port, filename, op='FOOBAR')
		if code != 403 and code < 500:
			return bcolors.test_fail
	return bcolors.test_pass

def test_200(host, port, opcode='GET'):
	print 'test 200s ' + opcode

	for filename in NORMAL_FILES:
		print '\t%s' % (filename)
		request, response, code = maybe_fetch(host, port, filename, op=opcode)
		if code != 200 or response is None:
			return bcolors.test_fail
		if opcode=='HEAD' and len(response.readlines()) != 0:
			return bcolors.test_fail
	return bcolors.test_pass

def test_404(host, port):
	print 'test 404s'
	for filename in NOTFOUNDS:
		print '\t%s' % (filename)
		request, response, code = maybe_fetch(host, port, filename)
		if code != 404 or response != None:
			return bcolors.test_fail
	return bcolors.test_pass

def test_301(host, port, opcode='GET'):
	print 'test 301s ' + opcode

	for filename, redirect in REDIRECTS:
		print '\t%s--->%s' % (filename, redirect)
		request, response, code = maybe_fetch(host, port, filename, op=opcode)
		# because we followed the redirect, the code should actually be 200,
		# and there should be a response.
		if code != 200 or response == None:
			return bcolors.test_fail
		if response.url != redirect:
			return bcolors.test_fail
	return bcolors.test_pass

def expected_response_type(fileExtension):
	if fileExtension == '.html':
		return 'text/html'
	elif fileExtension == '.txt':
		return 'text/plain'
	elif fileExtension == '.pdf':
		return 'application/pdf'
	elif fileExtension == '.png':
		return 'image/png'
	elif fileExtension in {'.jpeg', '.jpg'}:
		return 'image/jpeg'
	else:
		return ''

def test_response_type(host, port, opcode='GET'):
	print 'test Valid Response Type ' + opcode
	isPass = True
	for filename in NORMAL_FILES:
		request, response, code = maybe_fetch(host, port, filename, op=opcode)
		if response.headers.type != expected_response_type(os.path.splitext(filename)[1]):
			print '\t' + filename + ' ' + response.headers.type + ' ' + bcolors.test_fail
			isPass = False
		else:
			print '\t' + filename + ' ' + response.headers.type + ' ' + bcolors.test_pass
	if isPass:
		return bcolors.test_pass
	else:
		return bcolors.test_fail

def parse_flags(argv):
	arg_map = {}
	if len(argv) <= 1: return {}
	for arg in argv[1:]:
		bits = arg.split('=')
		if len(bits) == 2:
			arg_map[bits[0]] = bits[1]
	return arg_map
		
if __name__  == '__main__':
	arg_map = parse_flags(sys.argv)
	if ('--host' not in arg_map) or ('--port' not in arg_map):
		print 'usage: project1_testclient.py --host=linux2 --port=12345'
		sys.exit(-1)
	host = arg_map['--host']
	port = arg_map['--port']	

	print test_200(host, port) + '\n'
	print test_404(host, port) + '\n'
	print test_301(host, port) + '\n'
	print test_200(host, port, opcode='HEAD') + '\n'
	print test_301(host, port, opcode='HEAD') + '\n'
	print test_POST(host, port) + '\n'
	print test_INVALID(host, port) + '\n'
	print test_response_type(host, port) + '\n'
