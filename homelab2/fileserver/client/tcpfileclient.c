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
    char result[BUFSIZE];
    int filename_len;
    char filename1[255];
    char num_bytes[10];
    FILE *fp;

    /* check command line arguments */
    if (argc != 3) {
        fprintf(stderr, "usage: %s <hostname> <port>\n", argv[0]);
        exit(0);
    }
    hostname = argv[1];
    portno = atoi(argv[2]);

    /* gethostbyname: get the server's DNS entry */
    server = gethostbyname(hostname);
    if (server == NULL) {
        fprintf(stderr, "ERROR, no such host as %s\n", hostname);
        exit(0);
    }

    /* build the server's Internet address */
    bzero((char *) &serveraddr, sizeof(serveraddr));
    serveraddr.sin_family = AF_INET;
    bcopy((char *)server->h_addr,
          (char *)&serveraddr.sin_addr.s_addr, server->h_length);
    serveraddr.sin_port = htons(portno);

    /* socket: create the socket */
    sockfd = socket(AF_INET, SOCK_STREAM, 0);
    if (sockfd < 0)
        error("ERROR opening socket");

    /* connect: create a connection with the server */
    if (connect(sockfd, &serveraddr, sizeof(serveraddr)) < 0)
        error("ERROR connecting");

    char* hostaddrp = inet_ntoa(serveraddr.sin_addr);
    printf("Connected to %s:%d\n", hostaddrp, portno);

    while(1){
        /* get message line from the user */
        printf("Enter filename: ");
        bzero(buf, BUFSIZE);
        fgets(buf, BUFSIZE, stdin);

        filename_len = strlen(buf);

        strncpy(filename1, buf, filename_len);
        filename1[filename_len-1]='\0';
        strcat(filename1, "1");
        buf[filename_len-1] = '/';
        buf[filename_len] = '\0';

        printf("Enter number of bytes to be read: ");
        bzero(num_bytes, 10);
        fgets(num_bytes, 10, stdin);
        strcat(buf, num_bytes);

        /* send the message line to the server */
        n = write(sockfd, buf, strlen(buf));
        if (n < 0)
            error("ERROR writing to socket");

        /* print the server's reply */
        bzero(result, BUFSIZE);
        n = read(sockfd, result, BUFSIZE);
        if (n < 0)
            error("ERROR reading from socket");

        if(strcmp(result, "SORRY!")==0){
            printf("Server says that the file does not exist.\n");
        }
        else{

            fp = fopen(filename1, "w");

            if(strncmp(result, "SORRY!-", 7)==0){
                result[6]='\0';
                fprintf(fp, "%s%s", result, &result[7]);
            }
            else{
                fprintf(fp, "%s", result);
            }
            fclose(fp);
            printf("File received from server successfully: %s\n", filename1);
        }
    }
    
    close(sockfd);

    return 0;
}
