/* UDPtimed.c - main */
/* Modified by Krishna Sivalingam, IITM */
/* Combined multiple function definitions from different files into this file */
/* Jan. 16, 2017 */

#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>

#include <stdio.h>
#include <time.h>
#include <string.h>
#include <stdlib.h>
#include <netdb.h>
#include <sys/errno.h>
extern int	errno;

#define	UNIXEPOCH	2208988800UL	/* UNIX epoch, in UCT secs	*/

int	errexit(const char *format, ...);

unsigned short	portbase = 0;	/* port base, for non-root servers	*/

/*------------------------------------------------------------------------
 * passivesockUDP - allocate & bind a server socket using UDP
 *------------------------------------------------------------------------
 */
int
passivesockUDP(const char *service)
/*
 * Arguments:
 *      service   - service associated with the desired port
 */
{
	struct servent	*pse;	/* pointer to service information entry	*/
	struct protoent *ppe;	/* pointer to protocol information entry*/
	struct sockaddr_in sin;	/* an Internet endpoint address		*/
	int	s, type;	/* socket descriptor and socket type	*/

	memset(&sin, 0, sizeof(sin));
	sin.sin_family = AF_INET;
	sin.sin_addr.s_addr = INADDR_ANY;

	/* Map service name to port number */
	if ((pse = getservbyname(service, "udp")) != NULL)
	  sin.sin_port = htons(ntohs((unsigned short)pse->s_port)
			       + portbase);
	else if ((sin.sin_port=htons((unsigned short)atoi(service))) == 0)
	  errexit("can't get \"%s\" service entry\n", service);
	
	/* Map protocol name to protocol number */
	if ((ppe = getprotobyname("udp")) == NULL)
	  errexit("can't get \"%s\" bprotocol entry\n", "udp");

	type = SOCK_DGRAM;	/* For UDP */

	/* Allocate a socket */
	s = socket(PF_INET, type, ppe->p_proto);
	if (s < 0)
	  errexit("can't create socket: %s\n", strerror(errno));
	
	/* Bind the socket */
	if (bind(s, (struct sockaddr *)&sin, sizeof(sin)) < 0)
		errexit("can't bind to %s port: %s\n", service,
			strerror(errno));
	return s;
}



/*------------------------------------------------------------------------
 * main - Iterative UDP server for TIME service
 *------------------------------------------------------------------------
 */
int
main(int argc, char *argv[])
{
	struct sockaddr_in fsin;	/* the from address of a client	*/
	char	*service = "time";	/* service name or port number	*/
	char	buf[1];			/* "input" buffer; any size > 0	*/
	int	sock;			/* server socket		*/
	time_t	now;			/* current time			*/
	unsigned int	alen;		/* from-address length		*/

	switch (argc) {
	case	1:
		break;
	case	2:
		service = argv[1];
		break;
	default:
		errexit("usage: UDPtimed [port]\n");
	}

	sock = passivesockUDP(service);

	while (1) {
	  alen = sizeof(fsin);
	  
	  if (recvfrom(sock, buf, sizeof(buf), 0,
		       (struct sockaddr *)&fsin, &alen) < 0)
	    errexit("recvfrom: %s\n", strerror(errno));
	  
	  (void) time(&now);
	  
	  now = htonl((unsigned long)(now + UNIXEPOCH));
	  
	  (void) sendto(sock, (char *)&now, sizeof(now), 0,
			(struct sockaddr *)&fsin, sizeof(fsin));
	}
}
