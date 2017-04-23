/*
 * tcpserver.c - A simple TCP echo server
 * usage: tcpserver <port>
 */

#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <sys/signal.h>
#include <string.h>
#include <netdb.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#define BUFSIZE 1024

/*
 * error - wrapper for perror
 */
void error(char *msg) {
    perror(msg);
    exit(1);
}

int ServeFiles(int fd);
char* compute(char* input, int len);
void reaper(int sig);

int main(int argc, char **argv) {
    int parentfd; /* parent socket */
    int childfd; /* child socket */
    int portno; /* port to listen on */
    int clientlen; /* byte size of client's address */
    struct sockaddr_in serveraddr; /* server's addr */
    struct sockaddr_in clientaddr; /* client addr */
    struct hostent *hostp; /* client host info */
    char *hostaddrp; /* dotted decimal host addr string */
    int optval; /* flag value for setsockopt */
    int n; /* message byte size */
    int pid;

    /*
     * check command line arguments
     */
    if (argc != 2) {
        fprintf(stderr, "usage: %s <port>\n", argv[0]);
        exit(1);
    }
    portno = atoi(argv[1]);

    /*
     * socket: create the parent socket
     */
    parentfd = socket(AF_INET, SOCK_STREAM, 0);
    if (parentfd < 0)
        error("ERROR opening socket");

    (void) signal(SIGCHLD, reaper);

    /* setsockopt: Handy debugging trick that lets
     * us rerun the server immediately after we kill it;
     * otherwise we have to wait about 20 secs.
     * Eliminates "ERROR on binding: Address already in use" error.
     */
    optval = 1;
    setsockopt(parentfd, SOL_SOCKET, SO_REUSEADDR,
               (const void *)&optval , sizeof(int));

    /*
     * build the server's Internet address
     */
    bzero((char *) &serveraddr, sizeof(serveraddr));

    /* this is an Internet address */
    serveraddr.sin_family = AF_INET;

    /* let the system figure out our IP address */
    serveraddr.sin_addr.s_addr = htonl(INADDR_ANY);

    /* this is the port we will listen on */
    serveraddr.sin_port = htons((unsigned short)portno);

    /*
     * bind: associate the parent socket with a port
     */
    if (bind(parentfd, (struct sockaddr *) &serveraddr,
             sizeof(serveraddr)) < 0)
        error("ERROR on binding");

    /*
     * listen: make this socket ready to accept connection requests
     */
    if (listen(parentfd, 5) < 0) /* allow 5 requests to queue up */
        error("ERROR on listen");

    /*
     * main loop: wait for a connection request, echo input line,
     * then close connection.
     */
    clientlen = sizeof(clientaddr);
    while (1) {
        /*
         * accept: wait for a connection request
         */
        childfd = accept(parentfd, (struct sockaddr *) &clientaddr, &clientlen);
        if (childfd < 0)
            error("ERROR on accept");

        pid = fork();

        if(pid){ // parent
            close(childfd);
        }
        else
        if(pid==0) // child
        {
            close(parentfd);
            exit(ServeFiles(childfd));
        }
        else // error
        {
            error("Error in forking new process.");
        }
    }
}

/*
 *   Read from socket
 */

int ServeFiles(int fd){
    /*
     * read: read input string from the client
     */
    char buf[BUFSIZE]; /* message buffer */
    int n;
    char* result;

    while((n = read(fd, buf, BUFSIZE))>0){
        if (n < 0)
            error("ERROR reading from socket");

        result = compute(buf, n);

        /*
         * write: echo the input string back to the client
         */
        n = write(fd, result, strlen(result));
        if (n < 0)
            error("ERROR writing to socket");

        bzero(buf, BUFSIZE);
    }

    return 0;
}

/*
 * Parse message, read file as string 
 */

char* compute(char* input, int len){
    int i = len-1;
    while(i>=0){
        if(input[i]=='/')
            break;
        i--;
    }
    if(i==-1)
        return;
    char filename[256];
    int bytes;
    strncpy(filename, input, i);
    filename[i] = '\0';
    sscanf(&input[i+1], "%d", &bytes);

    FILE *fp = fopen(filename, "r");

    if(fp==NULL){
        return "SORRY!";
    }

    char *result = (char*)malloc((bytes+2)*sizeof(char));
    char *final = (char*)malloc((2*bytes+2)*sizeof(char));

    fgets(result, bytes, fp);

    fclose(fp);

    if(strncmp(result,"SORRY!", 6)==0){
        strcpy(final, "SORRY!-");
        strcat(final, &result[6]);
    }
    else
    {
        strcpy(final, result);
    }
    
    return final;
}

/*------------------------------------------------------------------------
 * reaper - clean up zombie children
 *------------------------------------------------------------------------
 */
void
reaper(int sig)
{
    int status;

    while (wait3(&status, WNOHANG, (struct rusage *)0) >= 0)
        /* empty */;
}