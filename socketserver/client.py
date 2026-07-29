import socket

HOST = '127.0.0.1'  # El IP del servidor (cambia si es diferente)
PORT = 1802        # El puerto en el que escucha el servidor

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    s.connect((HOST, PORT))
    s.send(b'Hello, world')
    s.send(b'0')
    data = s.recv(1024)

print(f'Received:{data!r}')