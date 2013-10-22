/*
 * <copyright>
 *  
 *  Copyright 1997-2004 Mobile Intelligence Corp
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.core.service.community;

import java.util.Collection;

import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;

import org.cougaar.core.component.Service;

/**
 * This service provides access to community capabilities.
 * <p>
 * Most operations are performed asynchronously in which case a
 * callback is used to return status and results.  In addition to
 * the asynchronous callback, the getCommunity and searchCommunity
 * methods may also return results immediately if the operation
 * can be completed using locally cached data.  In these cases the
 * callback is not invoked.  When required, callbacks are always
 * invoked from within a blackboard transaction.
 */
public interface CommunityService extends Service {

  // Entity types
  public static final int AGENT = 0;
  public static final int COMMUNITY = 1;

  /**
   * Request to create a community.  If the specified community does not
   * exist it will be created and the caller will become the community
   * manager.  It the community already exists a descriptor is obtained
   * from its community manager and returned in the response.
   * @param communityName    Name of community to create
   * @param attrs            Attributes to associate with new community
   * @param crl              Listener to receive response
   * @deprecated Use joinCommunity method with createIfNotFound argument
   *             set to true
   */
  void createCommunity(String                    communityName,
                       Attributes                attrs,
                       CommunityResponseListener crl);

  /**
   * Request to join a named community.  If the specified community does not
   * exist it may be created in which case the caller becomes the community
   * manager.  It the community doesn't exist and the caller has set the
   * "createIfNotFound flag to false the join request will be queued until the
   * community is found.
   * @param communityName    Community to join
   * @param entityName       New member name
   * @param entityType       Type of member entity to create (AGENT or COMMUNITY)
   * @param entityAttrs      Attributes for new member
   * @param createIfNotFound Create community if it doesn't exist
   * @param newCommunityAttrs   Attributes for created community (used if
   *                         createIfNotFound set to true, otherwise ignored)
   * @param crl              Listener to receive response
   */
  void joinCommunity(String                    communityName,
                     String                    entityName,
                     int                       entityType,
                     Attributes                entityAttrs,
                     boolean                   createIfNotFound,
                     Attributes                newCommunityAttrs,
                     CommunityResponseListener crl);

  /**
   * Request to leave named community.
   * @param communityName  Community to leave
   * @param entityName     Entity to remove from community
   * @param crl            Listener to receive response
   */
  void leaveCommunity(String                    communityName,
                      String                    entityName,
                      CommunityResponseListener crl);

  /**
   * Request to get a Community object.  If community is found in local cache
   * a reference is returned by method call.  If the community
   * is not found in the cache a null value is returned and the Community
   * object is requested from community's manager.  After the Community
   * object has been obtained from the community manager the supplied
   * CommunityResponseListener callback is invoked to notify the requester.
   * Note that the supplied callback is not invoked if a non-null value is
   * returned.
   * @param communityName  Community of interest
   * @param crl            Listener to receive response after remote fetch
   * @return               Community instance if found in cache or null if not
   *                       found
   */
  Community getCommunity(String                    communityName,
                         CommunityResponseListener crl);

  /**
   * Request to modify an Entity's attributes.
   * @param communityName    Name of community
   * @param entityName       Name of affected Entity or null if modifying
   *                         community attributes
   * @param mods             Attribute modifications
   * @param crl              Listener to receive response
   */
  void modifyAttributes(String                    communityName,
                        String                    entityName,
                        ModificationItem[]        mods,
                        CommunityResponseListener crl);

  /**
   * Initiates a community search operation.  The serach is performed against
   * the specified community or all communities of the calling agent
   * if the communityName argument is null.  The results of the search are
   * immediately returned as part of the method call if the search can be
   * resolved using locally cached data.  However, if the search requires
   * interaction with a remote community manager a null value is returned by
   * the method call and the search results are returned via the
   * CommunityResponseListener callback after the remote operation has been
   * completed.  In the case where the search can be satisified using local
   * data (i.e., the method returns a non-null value) the
   * CommunityResponseListener is not invoked.
   * @param communityName   Name of community to search or null to globally
   *                        search parent communities of calling agent
   * @param searchFilter    JNDI compliant search filter
   * @param recursiveSearch True for recursive search into nested communities
   *                        [false = search top community only]
   * @param resultQualifier Type of entities to return in result [ALL_ENTITIES,
   *                        AGENTS_ONLY, or COMMUNITIES_ONLY]
   * @param crl             Callback object to receive search results
   * @return                Collection of Entity objects matching search
   *                        criteria if available in local cache.  A null value
   *                        is returned if cache doesn't contained named
   *                        community.
   */
  Collection searchCommunity(String                    communityName,
                             String                    searchFilter,
                             boolean                   recursiveSearch,
                             int                       resultQualifier,
                             CommunityResponseListener crl);


  /**
   * Performs attribute based search of community entities.  This is a general
   * purpose search operation using a JNDI search filter.  This method is
   * non-blocking.  An empty Collection will be returned if the local cache is
   * empty.  Updated search results can be obtained by using the addListener
   * method to receive change notifications.
   * @param communityName Name of community to search
   * @param filter        JNDI search filter
   * @return              Collection of MessageAddress objects
   */
  Collection search(String communityName,
                    String filter);

  /**
   * Requests a collection of community names identifying the communities that
   * contain the specified member.  If the member name is null the immediate
   * parent communities for calling agent are returned.  If member is
   * the name of a nested community the names of all immediate parent communities
   * is returned.  The results are returned directly if the member name is
   * null or if a copy of the specified community is available in local cache.
   * Otherwise, the results will be returned in the CommunityResponseListener
   * callback in which case the method returns a null value.
   * @param member   Member name
   * @param crl    Listener to receive results if remote lookup is required
   * @return A collection of community names if operation can be resolved using
   *         data from local cache, otherwise null
   */
  Collection listParentCommunities(String                    member,
                                   CommunityResponseListener crl);

  /**
   * Requests a collection of community names identifying the communities that
   * contain the specified member and satisfy a given set of attributes.
   * The results are returned directly if the member name is
   * null or if a copy of the specified community is available in local cache.
   * Otherwise, the results will be returned in the CommunityResponseListener
   * callback in which case the method returns a null value.
   * @param member   Member name
   * @param filter Search filter defining community attributes
   * @param crl Listener to receive results
   * @return A collection of community names if operation can be resolved using
   *         data from local cache, otherwise null
   */
  Collection listParentCommunities(String                    member,
                                   String                    filter,
                                   CommunityResponseListener crl);

  /**
   * Invokes callback when specified community is found.
   * @param communityName Name of community
   * @param fccb          Callback invoked after community is found or timeout
   *                      has lapsed
   * @param timeout       Length of time (in milliseconds) to wait for
   *                      community to be located.  A value of -1 disables
   *                      the timeout.
   */
  void findCommunity(String                communityName,
                     FindCommunityCallback fccb,
                     long                  timeout);

  /**
   * Lists all communities in bound in White Pages.  Results are returned
   * in CommunityResponseListener callback.  The crl.getContent() method
   * returns a Collection of community names found in white pages.
   */
  void listAllCommunities(CommunityResponseListener crl);

  /**
   * Add listener for CommunityChangeEvents.
   * @param l  Listener
   */
  void addListener(CommunityChangeListener l);

  /**
   * Remove listener for CommunityChangeEvents.
   * @param l  Listener
   */
  void removeListener(CommunityChangeListener l);

  /**
   * Returns an array of community names of all communities of which caller is
   * a member.
   * @param allLevels Set to false if the list should contain only those
   *                  communities in which the caller is explicitly
   *                  referenced.  If true the list will also include those
   *                  communities in which the caller is implicitly a member
   *                  as a result of community nesting.
   * @return          Array of community names
   * @deprecated      This method will be removed in 11.2.
   *                  @see #listParentCommunities(String, CommunityResponseListener)
   */
  String[] getParentCommunities(boolean allLevels);

  /**
   * Lists all communities in Name Server.
   * @return  Collection of community names
   * @deprecated      This method will be removed in 11.2.
   *                  @see #listAllCommunities(CommunityResponseListener)
   */
  Collection listAllCommunities();

  /**
   * Requests a collection of community names identifying the communities that
   * contain the specified member.
   * @param member  Member name
   * @return      A collection of community names
   * @deprecated  This method will be removed in 11.2.
   *              @see #listParentCommunities(String, CommunityResponseListener)
   */
  Collection listParentCommunities(String member);

  /**
   * Requests a collection of community names identifying the communities that
   * contain the specified member and satisfy a given set of attributes.
   * @param member   Member name
   * @param filter Search filter defining community attributes
   * @return       A collection of community names
   * @deprecated   This method will be removed in 11.2.
   *               @see #listParentCommunities(String, String, CommunityResponseListener)
   */
  Collection listParentCommunities(String member, String filter);

}
