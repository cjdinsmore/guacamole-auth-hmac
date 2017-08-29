import json
import time
import uuid
import hmac
import hashlib
import base64
import requests
import sys

if len(sys.argv) < 7:
    print "Usage: python %s <server_url> <secret> <protocol> <target_ip> <username> <port> [<passwd>]" % sys.argv[0]
    print "passwd is optional for SSH, required for VNC."

guac_server = sys.argv[1]
secret = sys.argv[2]
protocol = sys.argv[3]
ip_address = sys.argv[4]
username = sys.argv[5]
port = sys.argv[6]

# passwd not required for SSH
if len(sys.argv) == 8:
    passwd = sys.argv[7]

# Create UUID for connection ID
conn_id = str(uuid.uuid4())
base64_conn_id = base64.b64encode(conn_id[2:] + "\0" + 'c' + "\0" + 'hmac')

# Create timestamp that looks like: 1489181545018
timestamp = str(int(round(time.time()*1000)))

# Concatenate info for a message
message = timestamp + protocol + ip_address + port + username + passwd

# Hash the message into a signature
signature = hmac.new(secret, message, hashlib.sha256).digest().encode("base64").rstrip('\n')

# Build the POST request
# Additional parameters from Guacamole docs can be added with "guac." prefix
request_string = ('timestamp=' + timestamp
                  + '&guac.port=' + port
                  + '&guac.username=' + username
                  + '&guac.password=' + passwd
                  + '&guac.protocol=' + protocol
                  + '&signature=' + signature
                  + '&guac.hostname=' + ip_address
                  + '&id=' + conn_id)

# Send request to Guacamole backend and record the result
response = requests.post(guac_server + '/api/tokens', data=request_string)

if response.status_code == 403:
    print "Guacamole server did not accept authentication."

token = json.loads(response.content)['authToken']
print guac_server + '/#/client/' + base64_conn_id + '?token=' + token
