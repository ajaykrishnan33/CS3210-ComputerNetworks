/* TCPecho.c - main, TCPecho */
/* Modified by Krishna Sivalingam, IITM */
/* Combined multiple function definitions from different files into this file */
/* Jan. 16, 2017 */

#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#include <netdb.h>
#include <sys/errno.h>

#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <time.h>

extern int	errno;
#define h_addr h_addr_list[0] /* for backward compatibility */
void	TCPecho(const char *host, const char *service);
int	errexit(const char *format, ...);
int	connectsockTCP(const char *host, const char *service);

#define	LINELEN		128

/*------------------------------------------------------------------------
 * connectsockTCP - allocate & connect a socket using TCP
 *------------------------------------------------------------------------
 */
int
connectsockTCP(const char *host, const char *service)
/*
 * Arguments:
 *      host      - name of host to which connection is desired
 *      service   - service associated with the desired port
 */
{
	struct hostent	*phe;	/* pointer to host information entry	*/
	struct servent	*pse;	/* pointer to service information entry	*/
	struct protoent *ppe;	/* pointer to protocol information entry*/
	struct sockaddr_in sin;	/* an Internet endpoint address		*/
	int	s, type;	/* socket descriptor and socket type	*/
	char transport[10];

	strcpy(transport, "tcp");

	memset(&sin, 0, sizeof(sin));
	sin.sin_family = AF_INET;

	/* Map service name to port number */
	if ( (pse = getservbyname(service, transport)) != NULL )
		sin.sin_port = pse->s_port;
	else if ((sin.sin_port=htons((unsigned short)atoi(service))) == 0)
		errexit("can't get \"%s\" service entry\n", service);

	/* Map host name to IP address, allowing for dotted decimal */
	if ( (phe = gethostbyname(host)) != NULL )
	  memcpy(&sin.sin_addr, phe->h_addr, phe->h_length);
	else if ( (sin.sin_addr.s_addr = inet_addr(host)) == INADDR_NONE )
	  errexit("can't get \"%s\" host entry\n", host);

	/* Map transport protocol name to protocol number */
	if ( (ppe = getprotobyname(transport)) == NULL)
	  errexit("can't get \"%s\" protocol entry\n", transport);
	
	type = SOCK_STREAM;

	/* Allocate a socket */
	s = socket(PF_INET, type, ppe->p_proto);
	if (s < 0)
	  errexit("can't create socket: %s\n", strerror(errno));
	
	/* Connect the socket */
	if (connect(s, (struct sockaddr *)&sin, sizeof(sin)) < 0)
	  errexit("can't connect to %s.%s: %s\n", host, service,
		  strerror(errno));
	return s;
}


/*------------------------------------------------------------------------
 * main - TCP client for ECHO service
 *------------------------------------------------------------------------
 */
int
main(int argc, char *argv[])
{
	char	*host = "localhost";	/* host to use if none supplied	*/
	char	*service = "echo";	/* default service name		*/

	switch (argc) {
	case 1:
	  host = "localhost";
	  break;
	case 3:
	  service = argv[2];
	  /* FALL THROUGH */
	case 2:
	  host = argv[1];
	  break;
	default:
	  fprintf(stderr, "usage: TCPecho [host [port]]\n");
	  exit(1);
	}
	TCPecho(host, service);
	exit(0);
}

/*------------------------------------------------------------------------
 * TCPecho - send input to ECHO service on specified host and print reply
 *------------------------------------------------------------------------
 */
void
TCPecho(const char *host, const char *service)
{
	char	buf[LINELEN+1];		/* buffer for one line of text	*/
	int	s, n;			/* socket descriptor, read count*/
	int	outchars, inchars;	/* characters sent and received	*/

	s = connectsockTCP(host, service);
	fprintf(stdout,"Input a line: ");

	while (fgets(buf, sizeof(buf), stdin)) {
	  buf[LINELEN] = '\0';	/* insure line null-terminated	*/
	  outchars = strlen(buf);
	  (void) write(s, buf, outchars);
	  
	  /* read it back */
	  for (inchars = 0; inchars < outchars; inchars+=n ) {
	    n = read(s, &buf[inchars], outchars - inchars);
	    if (n < 0)
	      errexit("socket read failed: %s\n",
		      strerror(errno));
	  }
	  fputs(buf, stdout);
	  fprintf(stdout,"Input a line: ");
	}

}
