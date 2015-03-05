#!/usr/bin/python

import socket
import string
import sys
import time
import urllib2
from urllib2 import urlopen,HTTPError

TIMEOUT = 5  # seconds
NORMAL_FILES = ['/index.html', '/foo/bar.html']
REDIRECTS = [['/cats', 'http://en.wikipedia.org/wiki/Cat']]
NOTFOUNDS = ['/redirect.defs', '/not/a/real/url.html']

# prevents us from following redirects, so we can get the HTTP response code
class DontFollowHttpRedirectHandler(urllib2.HTTPRedirectHandler):
	def http_error_307(self, req, fp, code, msg, headers):
		raise urllib2.HTTPError(req.get_full_url(), code, msg, headers, fp)
	http_error_302 = http_error_303 = http_error_307 


class NonPersistentTestRunner:
	def __init__(self, host, port, scheme='http'):
		self._host = host
		self._port = port
		self._scheme = scheme
  
	def _build_url(self, filename):
		return '%s://%s:%s%s' % (self._scheme, self._host, self._port, filename)

	# returns request, response, code
	# response is null for 4xx or 5xx response
	def _maybe_fetch(self, filename, op='GET'):
		request = urllib2.Request(self._build_url(filename))
		request.get_method = lambda : op
		opener = urllib2.build_opener(DontFollowHttpRedirectHandler)
		response = None
		try:
			response = opener.open(request)
		except HTTPError as e:
			return request, None, e.code
		return request, response, response.code

	def test_POST(self):
		for filename in NORMAL_FILES:
			request, response, code = self._maybe_fetch(filename, op='POST')
			if code != 403:
				return False
		return True

	def test_INVALID(self):
		for filename in NORMAL_FILES:
			request, response, code = self._maybe_fetch(filename, op='FOOBAR')
			if code != 403 and code < 500:
				return False
		return True

	def test_200(self, opcode='GET'):
		for filename in NORMAL_FILES:
			request, response, code = self._maybe_fetch(filename, op=opcode)
			if code != 200 or response is None:
				return False
			if opcode=='HEAD' and len(response.readlines()) != 0:
				return False
		return True

	def test_404(self):
		for filename in NOTFOUNDS:
			request, response, code = self._maybe_fetch(filename)
			if code != 404 or response != None:
				return False
		return True

	def test_301(self, opcode='GET'):
		for filename, redirect in REDIRECTS:
			request, response, code = self._maybe_fetch(filename, op=opcode)
			# because we followed the redirect, the code should actually be 200,
			# and there should be a response.
			if code != 200 or response == None:
				return False
			if response.url != redirect:
				return False
		return True

	def run_all_tests(self):
		results = {}

		test_list = {
			'200' : (self.test_200, None),
			'404' : (self.test_404, None),
			'301' : (self.test_301, None),
			'200 HEAD' : (self.test_200, 'HEAD'),
			'301 HEAD' : (self.test_301, 'HEAD'),
			'POST' : (self.test_POST, None),
			'INVALID' : (self.test_INVALID, None),
		}
	
		for test_case in test_list:
			func, op = test_list[test_case]
			try:
				if op:
					results[test_case] = func(opcode=op)
				else:
					results[test_case] = func()
			except Exception as e:
				results[test_case] = False
		return results


class PersistentTestRunner:
	def __init__(self, host, port):
		self._host = host
		self._port = port

	def test_200(self):
		try:
			sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
		except socket.error, msg:
			print 'error creating a TCP socket!', msg[1]

		try:
			sock.connect((self._host, int(self._port)))
			sock.settimeout(1.0)
		except socket.error, msg:
			print 'error connecting to %s:%s' % (self._host, self._port)

		results = {}
		filename = '/index.html'
		url = 'http://%s:%s%s' % (self._host, self._port, filename)
		response = ''
		try:
			sock.send("GET %s HTTP/1.1\r\nConnection: Keep-Alive\r\nHost: %s:%s\r\n\r\n" % (filename, self._host, self._port))
			sock.send("GET %s HTTP/1.1\r\nConnection: Close\r\nHost: %s:%s\r\n\r\n" % (filename, self._host, self._port))
			data = sock.recv(1024)
			while len(data):
				response = response + data
				data = sock.recv(1024)
		except Exception as e:
			print "Timeout", e
			print 'response: ' + response
			print "[" + response + "]"
			print len(response)
		sock.close()

		if not response:
			print 'response is None or empty, FAIL'
			results[filename] = False

		upper_response = string.upper(response)
		if 2 != upper_response.count('HTTP/1.1 200 OK'):
			print 'Did not find 2 successful responses.'
			results[filename] = False
		elif 2 != upper_response.count('CONNECTION: KEEP-ALIVE'):
			print 'Did not find 2 keep alives!'
			results[filename] = False
		elif 0 != upper_response.count('CONNECTION: CLOSE'):
			print 'Server closed the connection, but requested keep-alive. FAIL'
			results[filename] = False
		else:
			results[filename] = True
		return results


class ExtraCreditTestRunner:
	def __init__(self, host, port, sslPort):
		self._host = host
		self._port = port
		self._sslPort = sslPort;

	def testSimultaneousConnections(self):
		try:
			sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
		except socket.error, msg:
			print 'error creating HTTP socket, FAIL', msg[1]
			return False
		
		try:
			sock.connect((self._host, int(self._port)))
		except socket.error, msg:
			print 'error connecting to HTTP socket, FAIL', msg[1]
			return False

		# leave the HTTP connection hanging, now that the server has returned from accept(),
		# and try the HTTPS connection
		response = urllib2.urlopen('https://%s:%s/index.html' % (self._host, int(self._sslPort)))
		return response.getcode() == 200
 
	
def parse_flags(argv):
	arg_map = {}
	if len(argv) <= 1: return {}
	for arg in argv[1:]:
		bits = arg.split('=')
		if len(bits) == 2:
			arg_map[bits[0]] = bits[1]
	return arg_map

# just iterate twice because this is a small list and it's simpler
def dump_results(result_map):
	print '\tPassing tests:'
	for label in result_map:
		if result_map[label]:
			print '\t\t' + label
	print '\tFailing tests:'
	for label in result_map:
		if not result_map[label]:
			print '\t\t' + label
	
		
if __name__  == '__main__':
	arg_map = parse_flags(sys.argv)
	if ('--host' not in arg_map) or ('--port' not in arg_map) or ('--sslport' not in arg_map):
		print 'usage: project2_autograder.py --host=linux2.cs.uchicago.edu --port=12345 --sslport=12346'
		sys.exit(-1)
	host = arg_map['--host']
	port = arg_map['--port']
	sslport = arg_map['--sslport']
	
	print 'HTTP tests!'
	dump_results(NonPersistentTestRunner(host, port, 'http').run_all_tests())

	print '\n\nHTTPS tests!'
	dump_results(NonPersistentTestRunner(host, sslport, 'https').run_all_tests())

	print '\n\nPersistent tests!'
	try:
		dump_results(PersistentTestRunner(host, port).test_200())
	except Exception as e:
		print e
		print 'Persistent tests FAILED due to exception.'
	print '\n\nExtra credit tests!'
	try:	
		extra_credit = ExtraCreditTestRunner(host, port, sslport).testSimultaneousConnections()
	except Exception as e:
		print e
		extra_credit = False		
	if extra_credit:
		print '\tPASSED'
	else:
		print '\tFAILED'
