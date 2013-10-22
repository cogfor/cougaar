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
package org.cougaar.community;

/**
 * Constants used by Community Service.
 */
public interface CommunityServiceConstants {

  // Community Manager request types
  public static final int UNDEFINED                    = -1;
  public static final int JOIN                         = 0;
  public static final int LEAVE                        = 1;
  public static final int MODIFY_ATTRIBUTES            = 2;
  public static final int GET_COMMUNITY_DESCRIPTOR     = 3;
  public static final int LIST                         = 4;

  public static final long NEVER                       = -1;

  // Defines how long CommunityDescriptor updates should be aggregated before
  // sending to interested agents.
  public static final String UPDATE_INTERVAL_PROPERTY =
      "org.cougaar.community.update.interval";
  public static long DEFAULT_UPDATE_INTERVAL = 5 * 1000;

  // Defines frequency of White Pages read to verify that an agent is still
  // manager for community
  public static final String VERIFY_MGR_INTERVAL_PROPERTY =
      "org.cougaar.community.manager.check.interval";
  public static long DEFAULT_VERIFY_MGR_INTERVAL = 1 * 60 * 1000;

  // Period that a client caches its community descriptors
  public static final String CACHE_EXPIRATION_PROPERTY =
      "org.cougaar.community.cache.expiration";
  public static long DEFAULT_CACHE_EXPIRATION = NEVER;

  // Classname of CommunityAccessManager to use for request authorization
  public static final String COMMUNITY_ACCESS_MANAGER_PROPERTY =
      "org.cougaar.community.access.manager.classname";
  public static String DEFAULT_COMMUNITY_ACCESS_MANAGER_CLASSNAME =
      "org.cougaar.community.manager.CommunityAccessManager";

  // Defines how often an agent will check parent communities to verify
  // correct state
  public static final String VERIFY_MEMBERSHIPS_INTERVAL_PROPERTY =
      "org.cougaar.community.verify.memberships.interval";
  public static long DEFAULT_VERIFY_MEMBERSHIPS_INTERVAL = 5 * 60 * 1000;

  // Defines whether community descriptors are returned with response from
  // community manager.
  public static final String INCLUDE_DESCRIPTOR_IN_RESPONSE_PROPERTY =
      "org.cougaar.community.manager.include.descriptor";
  public static boolean DEFAULT_INCLUDE_DESCRIPTOR_IN_RESPONSE = true;


}
