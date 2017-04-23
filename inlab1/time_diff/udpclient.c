/* 
 * tcpclient.c - A simple TCP client
 * usage: tcpclient <host> <port>
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h> 
#include <arpa/inet.h>
#include <time.h>

#define BUFSIZE 1024

/* 
 * error - wrapper for perror
 */
void error(char *msg) {
    perror(msg);
    exit(0);
}

int main(int argc, char **argv) {
    int sockfd, portno, n;
    struct sockaddr_in serveraddr;
    struct hostent *server;
    char *hostname;
    char buf[BUFSIZE];

    char *TEMP = "HAHAHA";

    /* check command line arguments */
    if (argc != 3) {
       fprintf(stderr,"usage: %s <hostname> <port>\n", argv[0]);
       exit(0);
    }
    hostname = argv[1];
    portno = atoi(argv[2]);

    /* gethostbyname: get the server's DNS entry */
    server = gethostbyname(hostname);
    if (server == NULL) {
        fprintf(stderr,"ERROR, no such host as %s\n", hostname);
        exit(0);
    }

    /* build the server's Internet address */
    bzero((char *) &serveraddr, sizeof(serveraddr));
    serveraddr.sin_family = AF_INET;
    bcopy((char *)server->h_addr, 
	  (char *)&serveraddr.sin_addr.s_addr, server->h_length);
    serveraddr.sin_port = htons(portno);

    int i = 0;
    time_t beg, end;
    time(&beg);
    while(i<10000){
        /* socket: create the socket */
        sockfd = socket(AF_INET, SOCK_DGRAM, 0);
        if (sockfd < 0) 
            error("ERROR opening socket");

        /* connect: create a connection with the server */
        if (connect(sockfd, &serveraddr, sizeof(serveraddr)) < 0) 
          error("ERROR connecting");

        // char* hostaddrp = inet_ntoa(serveraddr.sin_addr);
        // printf("Connected to %s:%d\n", hostaddrp, portno);

        /* get message line from the user */
        // printf("Please enter msg: ");
        bzero(buf, BUFSIZE);
        // fgets(buf, BUFSIZE, stdin);
        strcpy(buf, TEMP);

        /* send the message line to the server */
        n = write(sockfd, buf, strlen(buf));
        if (n < 0) 
          error("ERROR writing to socket");

        /* print the server's reply */
        bzero(buf, BUFSIZE);
        n = read(sockfd, buf, BUFSIZE);
        if (n < 0) 
          error("ERROR reading from socket");
        // printf("Echo from server: %s\n", buf);
        close(sockfd);
        i++;
    }
    time(&end);
    double diff = difftime(end, beg);

    printf("Time taken %f seconds\n", diff);
    
    return 0;
}
